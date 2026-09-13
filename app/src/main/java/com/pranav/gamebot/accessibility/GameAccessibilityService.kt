package com.pranav.gamebot.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Executes taps/swipes on screen without root, via AccessibilityService.dispatchGesture().
 * This is the "hands" of the bot — Perception/Decision layers never touch this directly,
 * they go through ActionExecutor which wraps this service.
 */
class GameAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "GameAccessibilityService"

        // Simple static reference so ActionExecutor can reach the running service.
        // (Fine for a single-purpose bot app; revisit if you need multi-instance safety.)
        var instance: GameAccessibilityService? = null
    }

    // dispatchGesture() silently returns false if a gesture is already in flight —
    // without this guard, overlapping calls (e.g. ExploreState firing pressRun()
    // immediately followed by moveDirection()) can quietly drop the second one.
    @Volatile private var gestureInProgress = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for input — perception happens via ScreenCaptureService instead.
    }

    override fun onInterrupt() {}

    private fun dispatch(gesture: GestureDescription, label: String) {
        if (gestureInProgress) {
            Log.w(TAG, "Dropped $label — previous gesture still in flight")
            return
        }
        gestureInProgress = true
        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                gestureInProgress = false
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                gestureInProgress = false
                Log.w(TAG, "$label cancelled")
            }
        }
        val accepted = dispatchGesture(gesture, callback, null)
        if (!accepted) {
            gestureInProgress = false
            Log.w(TAG, "$label rejected by dispatchGesture()")
        }
    }

    /** Single tap at (x, y). durationMs adds slight human-like variance. */
    fun tap(x: Float, y: Float, durationMs: Long = 60L) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        dispatch(GestureDescription.Builder().addStroke(stroke).build(), "tap")
    }

    /** Swipe from (x1,y1) to (x2,y2) — used for movement joystick / camera drag. */
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 200L) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        dispatch(GestureDescription.Builder().addStroke(stroke).build(), "swipe")
    }

    /** Press-and-hold at a point — e.g. holding the run button. */
    fun longPress(x: Float, y: Float, durationMs: Long = 800L) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        dispatch(GestureDescription.Builder().addStroke(stroke).build(), "longPress")
    }
}
