package com.pranav.gamebot.action

import com.pranav.gamebot.accessibility.GameAccessibilityService
import kotlin.random.Random

/**
 * Translates high-level intents ("open inventory", "run forward") into
 * low-level gestures on GameAccessibilityService.
 *
 * Screen coordinates below are PLACEHOLDERS — calibrate them per-game via
 * a one-time setup screen (tap where the button is, save the coordinate).
 */
object ActionExecutor {

    private fun service() = GameAccessibilityService.instance

    // --- Placeholder coordinates, replace with real calibrated values ---
    private var inventoryButton = Pair(980f, 1800f)
    private var runButton = Pair(950f, 1600f)
    private var joystickCenter = Pair(200f, 1700f)

    fun calibrate(inventory: Pair<Float, Float>? = null, run: Pair<Float, Float>? = null, joystick: Pair<Float, Float>? = null) {
        inventory?.let { inventoryButton = it }
        run?.let { runButton = it }
        joystick?.let { joystickCenter = it }
    }

    fun openInventory() {
        humanDelay()
        service()?.tap(inventoryButton.first, inventoryButton.second)
    }

    fun pressRun() {
        humanDelay()
        service()?.longPress(runButton.first, runButton.second, durationMs = 1000L)
    }

    /** direction in degrees, 0 = up/forward, clockwise */
    fun moveDirection(directionDegrees: Double, durationMs: Long = 500L) {
        humanDelay()
        val radius = 100f
        val rad = Math.toRadians(directionDegrees)
        val dx = (radius * Math.sin(rad)).toFloat()
        val dy = (-radius * Math.cos(rad)).toFloat()
        service()?.swipe(
            joystickCenter.first, joystickCenter.second,
            joystickCenter.first + dx, joystickCenter.second + dy,
            durationMs
        )
    }

    fun tapAt(x: Float, y: Float) {
        humanDelay()
        service()?.tap(x, y)
    }

    /** Small randomized delay so actions don't look perfectly robotic/scripted. */
    private fun humanDelay() {
        Thread.sleep(Random.nextLong(40, 140))
    }
}
