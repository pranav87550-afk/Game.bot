package com.pranav.gamebot

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pranav.gamebot.accessibility.GameAccessibilityService
import com.pranav.gamebot.capture.ScreenCaptureService
import com.pranav.gamebot.orchestrator.AgentLoop

/**
 * Entry point: requests the two permissions the bot needs (screen capture +
 * accessibility), binds ScreenCaptureService, and starts/stops AgentLoop.
 *
 * No XML layout yet — builds a tiny UI in code so this compiles standalone.
 * Swap for a real layout whenever you want a nicer screen.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var agentLoop: AgentLoop
    private var captureService: ScreenCaptureService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ScreenCaptureService.LocalBinder
            captureService = binder.getService()
            captureService?.onFrame = { bitmap -> agentLoop.onFrame(bitmap) }
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            captureService = null
            bound = false
        }
    }

    private val projectionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startCaptureService(result.resultCode, result.data!!)
        } else {
            statusText.text = "Screen capture permission denied"
        }
    }

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        agentLoop = AgentLoop(applicationContext)
        setContentView(buildUi())
    }

    private fun buildUi(): LinearLayout {
        statusText = TextView(this).apply {
            text = "Ready"
            setPadding(32, 32, 32, 32)
        }

        val enableAccessibilityBtn = Button(this).apply {
            text = "1. Enable Accessibility Service"
            setOnClickListener { openAccessibilitySettings() }
        }

        val requestCaptureBtn = Button(this).apply {
            text = "2. Grant Screen Capture + Start"
            setOnClickListener { requestScreenCapture() }
        }

        val stopBtn = Button(this).apply {
            text = "Stop"
            setOnClickListener { stopBot() }
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(statusText)
            addView(enableAccessibilityBtn)
            addView(requestCaptureBtn)
            addView(stopBtn)
        }
    }

    private fun openAccessibilitySettings() {
        if (isAccessibilityServiceEnabled()) {
            statusText.text = "Accessibility service already enabled"
        } else {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = "${packageName}/${GameAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(":").any { TextUtils.equals(it, expected) }
    }

    private fun requestScreenCapture() {
        if (!isAccessibilityServiceEnabled()) {
            statusText.text = "Enable the Accessibility Service first (step 1)"
            return
        }
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun startCaptureService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
        }
        startForegroundService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        agentLoop.start()
        statusText.text = "Running"
    }

    private fun stopBot() {
        agentLoop.stop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
        stopService(Intent(this, ScreenCaptureService::class.java))
        statusText.text = "Stopped"
    }

    override fun onDestroy() {
        if (bound) unbindService(connection)
        super.onDestroy()
    }
}
