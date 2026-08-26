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
    // Reference point representing "where the player character is" on screen —
    // used to compute bearing toward/away from a detected object. For most
    // games with a centered camera this is close to screen center.
    private var playerScreenPosition = Pair(540f, 960f)

    fun calibrate(
        inventory: Pair<Float, Float>? = null,
        run: Pair<Float, Float>? = null,
        joystick: Pair<Float, Float>? = null,
        playerPosition: Pair<Float, Float>? = null
    ) {
        inventory?.let { inventoryButton = it }
        run?.let { runButton = it }
        joystick?.let { joystickCenter = it }
        playerPosition?.let { playerScreenPosition = it }
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

    /**
     * Attack by tapping the target's on-screen position — the common pattern
     * for mobile survival/RPG games (tap-to-target or tap-to-attack). If your
     * game instead uses a fixed attack button + separate aiming, replace this
     * with: aim via moveDirection(bearingTo(targetPosition)) then tap a fixed
     * attack button coordinate.
     */
    fun attack(targetPosition: Pair<Float, Float>) {
        humanDelay()
        service()?.tap(targetPosition.first, targetPosition.second)
    }

    /**
     * Bearing in degrees (0 = up, clockwise) from the player's screen position
     * to a target point — feed this into moveDirection() to walk toward/away
     * from something detected on screen.
     */
    fun bearingTo(targetPosition: Pair<Float, Float>): Double {
        val dx = (targetPosition.first - playerScreenPosition.first).toDouble()
        // Screen Y grows downward, so invert for a standard "up = 0deg" bearing.
        val dy = (playerScreenPosition.second - targetPosition.second).toDouble()
        val degrees = Math.toDegrees(Math.atan2(dx, dy))
        return if (degrees < 0) degrees + 360 else degrees
    }

    /** Opposite direction of bearingTo() — useful for fleeing. */
    fun bearingAwayFrom(targetPosition: Pair<Float, Float>): Double {
        return (bearingTo(targetPosition) + 180) % 360
    }

    /** Small randomized delay so actions don't look perfectly robotic/scripted. */
    private fun humanDelay() {
        Thread.sleep(Random.nextLong(40, 140))
    }
}
