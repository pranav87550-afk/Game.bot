package com.pranav.gamebot.capture

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat

/**
 * Captures the device screen continuously and hands frames off to the
 * perception layer (DetectorEngine). Runs as a foreground + bound service:
 * bind to it from MainActivity/AgentLoop to set [onFrame] and receive Bitmaps.
 *
 * Wiring: start this with the Intent + resultCode from
 * MediaProjectionManager.createScreenCaptureIntent(), obtained in MainActivity.
 */
class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var screenWidth = 0
    private var screenHeight = 0

    // Throttle: perception doesn't need every frame at full display refresh rate.
    private var lastFrameTimeMs = 0L
    private val minFrameIntervalMs = 1000L / TARGET_FPS

    // Callback for perception layer (AgentLoop) to consume frames.
    var onFrame: ((Bitmap) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): ScreenCaptureService = this@ScreenCaptureService
    }
    private val binder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        const val CHANNEL_ID = "gamebot_capture"
        const val NOTIF_ID = 1
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val TARGET_FPS = 10 // enough for turn-based decisions; raise if reaction time needs it
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        handlerThread = HandlerThread("ScreenCaptureThread").apply { start() }
        backgroundHandler = Handler(handlerThread!!.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val resultData: Intent = intent.getParcelableExtra(EXTRA_RESULT_DATA) ?: return START_NOT_STICKY

        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

        setupVirtualDisplay()
        return START_STICKY
    }

    private fun setupVirtualDisplay() {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as android.view.WindowManager
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)

        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "GameBotCapture", screenWidth, screenHeight, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, backgroundHandler
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val now = System.currentTimeMillis()
            if (now - lastFrameTimeMs < minFrameIntervalMs) {
                // Drop this frame to hit target FPS instead of flooding the perception layer.
                reader.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            lastFrameTimeMs = now

            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val bitmap = imageToBitmap(image)
                onFrame?.invoke(bitmap)
            } finally {
                image.close()
            }
        }, backgroundHandler)
    }

    /**
     * Converts an RGBA_8888 Image from ImageReader into a Bitmap.
     * Handles row padding: ImageReader's rowStride is often wider than
     * width * pixelStride due to hardware alignment, so we crop it back down.
     */
    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val paddedBitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        paddedBitmap.copyPixelsFromBuffer(buffer)

        // Crop off the padding so downstream (detector, display) gets exact screen dimensions.
        return if (rowPadding == 0) {
            paddedBitmap
        } else {
            Bitmap.createBitmap(paddedBitmap, 0, 0, image.width, image.height).also {
                paddedBitmap.recycle()
            }
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Game.bot running")
            .setContentText("Capturing screen for perception")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Game.bot Capture", NotificationManager.IMPORTANCE_LOW
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        handlerThread?.quitSafely()
        super.onDestroy()
    }
}
