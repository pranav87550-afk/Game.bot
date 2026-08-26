package com.pranav.gamebot.orchestrator

import android.content.Context
import android.graphics.Bitmap
import com.pranav.gamebot.decision.StateMachine
import com.pranav.gamebot.perception.DetectorEngine
import com.pranav.gamebot.perception.GameState
import kotlinx.coroutines.*

/**
 * The main capture -> detect -> decide -> act loop.
 * Wire ScreenCaptureService.onFrame to call onFrame() here.
 */
class AgentLoop(context: Context) {

    private val detector = DetectorEngine(context)
    private val stateMachine = StateMachine()
    private var lastGameState = GameState()

    private var loopJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var running: Boolean = false
        private set

    fun start() {
        running = true
    }

    fun stop() {
        running = false
        loopJob?.cancel()
        detector.close()
    }

    /** Call this from ScreenCaptureService's frame callback. */
    fun onFrame(frame: Bitmap) {
        if (!running) return
        // Run detection off the capture thread so we never block frame delivery.
        scope.launch {
            val detections = detector.detect(frame)
            lastGameState = detector.toGameState(detections, lastGameState)
            withContext(Dispatchers.Main) {
                stateMachine.tick(lastGameState)
            }
        }
    }
}
