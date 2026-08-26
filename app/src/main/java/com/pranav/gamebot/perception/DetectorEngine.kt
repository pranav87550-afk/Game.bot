package com.pranav.gamebot.perception

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Wraps a TFLite object-detection model (place your .tflite file in
 * assets/models/detector.tflite, and its label list in
 * assets/models/labelmap.txt — one label per line, in class-index order).
 *
 * Assumes a standard SSD-MobileNet-style export with 4 outputs:
 *   output[0]: bounding boxes   [1, NUM_DETECTIONS, 4]  (top, left, bottom, right — normalized 0-1)
 *   output[1]: class indices    [1, NUM_DETECTIONS]
 *   output[2]: confidence scores[1, NUM_DETECTIONS]
 *   output[3]: number of detections [1]
 *
 * This is the standard format for TFLite object-detection models (e.g. models
 * exported via the TFLite Model Maker, or converted YOLO/SSD checkpoints).
 * If your model's export differs, adjust parseOutputs() accordingly.
 *
 * IMPORTANT: a generic pretrained model (COCO, etc.) will NOT know "enemy" /
 * "wood" / "stone" — those are game-specific classes. You'll need to either
 * fine-tune a small detector on screenshots from your target game, or start
 * with template-matching/color-detection for a v0 and swap in a trained
 * model once you have labeled data.
 */
class DetectorEngine(private val context: Context) {

    companion object {
        private const val MODEL_FILENAME = "models/detector.tflite"
        private const val LABELMAP_FILENAME = "models/labelmap.txt"
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val INPUT_SIZE = 320 // must match the model's expected input (common: 300 or 320)
        private const val MAX_DETECTIONS = 10
    }

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var modelInputSize = INPUT_SIZE

    init {
        try {
            interpreter = Interpreter(loadModelFile())
            labels = loadLabelMap()
            // Read actual expected input size from the model itself rather than assuming.
            interpreter?.getInputTensor(0)?.shape()?.let { shape ->
                if (shape.size >= 2) modelInputSize = shape[1]
            }
        } catch (e: Exception) {
            // No model present yet (e.g. fresh skeleton checkout) — detect() will
            // just return no detections until a real .tflite file is added.
            interpreter = null
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILENAME)
        val inputStream = assetFileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
    }

    private fun loadLabelMap(): List<String> {
        return try {
            context.assets.open(LABELMAP_FILENAME).use { stream ->
                BufferedReader(InputStreamReader(stream)).readLines().filter { it.isNotBlank() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Run inference on a single frame, return raw detections above CONFIDENCE_THRESHOLD. */
    fun detect(frame: Bitmap): List<Detection> {
        val interp = interpreter ?: return emptyList()

        val inputBuffer = preprocess(frame)

        val outputBoxes = Array(1) { Array(MAX_DETECTIONS) { FloatArray(4) } }
        val outputClasses = Array(1) { FloatArray(MAX_DETECTIONS) }
        val outputScores = Array(1) { FloatArray(MAX_DETECTIONS) }
        val outputCount = FloatArray(1)

        val outputs = mapOf(
            0 to outputBoxes,
            1 to outputClasses,
            2 to outputScores,
            3 to outputCount
        )

        interp.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

        return parseOutputs(outputBoxes[0], outputClasses[0], outputScores[0], outputCount[0], frame.width, frame.height)
    }

    /** Resize + normalize the frame into the ByteBuffer the interpreter expects. */
    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, modelInputSize, modelInputSize, true)

        // 4 bytes per channel (float32) x 3 channels (RGB)
        val buffer = ByteBuffer.allocateDirect(4 * modelInputSize * modelInputSize * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(modelInputSize * modelInputSize)
        resized.getPixels(pixels, 0, modelInputSize, 0, 0, modelInputSize, modelInputSize)

        for (pixel in pixels) {
            // Normalize 0-255 -> 0-1. If your model expects [-1, 1] instead,
            // change to: (value - 127.5f) / 127.5f
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255f) // R
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)  // G
            buffer.putFloat((pixel and 0xFF) / 255f)          // B
        }

        if (resized !== bitmap) resized.recycle()
        buffer.rewind()
        return buffer
    }

    private fun parseOutputs(
        boxes: Array<FloatArray>,
        classes: FloatArray,
        scores: FloatArray,
        count: Float,
        frameWidth: Int,
        frameHeight: Int
    ): List<Detection> {
        val detections = mutableListOf<Detection>()
        val numDetections = count.toInt().coerceIn(0, MAX_DETECTIONS)

        for (i in 0 until numDetections) {
            val score = scores[i]
            if (score < CONFIDENCE_THRESHOLD) continue

            val classIndex = classes[i].toInt()
            val label = labels.getOrNull(classIndex) ?: "class_$classIndex"

            // Model outputs normalized [top, left, bottom, right] in 0..1 — scale to actual frame pixels.
            val top = boxes[i][0] * frameHeight
            val left = boxes[i][1] * frameWidth
            val bottom = boxes[i][2] * frameHeight
            val right = boxes[i][3] * frameWidth

            detections.add(
                Detection(
                    label = label,
                    confidence = score,
                    boundingBox = RectF(left, top, right, bottom)
                )
            )
        }
        return detections
    }

    /** Fold raw detections into a structured GameState for the decision layer. */
    fun toGameState(detections: List<Detection>, previous: GameState = GameState()): GameState {
        val enemy = detections.firstOrNull { it.label == "enemy" }
        val resource = detections.firstOrNull { it.label in listOf("wood", "stone", "food") }

        return previous.copy(
            enemyVisible = enemy != null,
            enemyPosition = enemy?.let { Pair(it.boundingBox.centerX(), it.boundingBox.centerY()) },
            nearbyResource = resource?.label,
            nearbyResourcePosition = resource?.let { Pair(it.boundingBox.centerX(), it.boundingBox.centerY()) },
            timestampMs = System.currentTimeMillis()
        )
        // health/hunger typically read from a fixed HUD region via cropped
        // template match or a small classifier — add here once calibrated.
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
