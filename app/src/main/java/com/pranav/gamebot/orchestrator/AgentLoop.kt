package com.pranav.gamebot.orchestrator

import android.content.Context
import android.graphics.Bitmap
import com.pranav.gamebot.decision.StateMachine
import com.pranav.gamebot.perception.DetectorEngine
import com.pranav.gamebot.perception.GameState
import kotlinx.coroutines.*
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * The main capture -> detect -> decide -> act loop.
 * Wire ScreenCaptureService.onFrame to call onFrame() here.
 */
class AgentLoop(context: Context) {

    private val detector = DetectorEngine(context)
    private val stateMachine = StateMachine()
    private var lastGameState = GameState()

    private var loopJob: Job? = null
    // Dedicated single thread rather than the shared Dispatchers.Default pool:
    // ActionExecutor's small pacing delays block whatever thread runs tick(),
    // and we don't want that eating into the CPU-bound thread pool other
    // coroutines may share.
    private val agentDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(agentDispatcher)

    // Guards against piling up concurrent detect() calls: TFLite's Interpreter
    // is not safe to invoke from multiple threads at once, and if inference is
    // slower than the capture rate (likely on lower-end chips), frames would
    // otherwise queue up unbounded. Any frame arriving while one is still being
    // processed is simply dropped — the next frame a few ms later is fine.
    @Volatile private var processingFrame = false

    var running: Boolean = false
        private set

    fun start() {
        running = true
    }

    /**
     * Pauses the loop — frames arriving while stopped are dropped in onFrame().
     * Deliberately does NOT close the detector: the UI's Stop button lets the
     * user start() again later, and closing the TFLite interpreter here would
     * make every detection silently return empty forever after one stop/start
     * cycle. The interpreter is only torn down in [release].
     */
    fun stop() {
        running = false
        loopJob?.cancel()
    }

    /** Call once when the owning component (e.g. MainActivity) is destroyed for good. */
    fun release() {
        stop()
        detector.close()
        agentDispatcher.close()
    }

    /** Call this from ScreenCaptureService's frame callback. */
    fun onFrame(frame: Bitmap) {
        if (!running || processingFrame) {
            frame.recycle()
            return
        }
        processingFrame = true
        // Run detection off the capture thread so we never block frame delivery.
        // No need to hop to Main here — nothing in tick()/ActionExecutor touches
        // the UI, and doing so previously forced ActionExecutor's blocking
        // delays onto the main thread, risking jank/ANRs.
        scope.launch {
            try {
                val detections = detector.detect(frame)
                lastGameState = detector.toGameState(detections, lastGameState)
                stateMachine.tick(lastGameState)
            } finally {
                frame.recycle()
                processingFrame = false
            }
        }
    }
}
