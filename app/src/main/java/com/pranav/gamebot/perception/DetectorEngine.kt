package com.pranav.gamebot.perception

import android.content.Context
import android.graphics.Bitmap
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Wraps a TFLite object-detection model (place your .tflite file in
 * assets/models/). Converts a raw frame -> list of Detections -> GameState.
 */
class DetectorEngine(private val context: Context) {

    companion object {
        private const val MODEL_FILENAME = "models/detector.tflite"
        private const val CONFIDENCE_THRESHOLD = 0.5f
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

    fun detect(frame: Bitmap): List<Detection> {
        return emptyList()
    }

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
    }
}
