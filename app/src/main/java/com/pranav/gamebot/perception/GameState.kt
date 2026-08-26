package com.pranav.gamebot.perception

/**
 * Structured snapshot of "what the bot currently sees", built from raw
 * detections each frame. Decision layer reads ONLY this object — it never
 * touches pixels directly. Keeps perception and decision cleanly separated.
 */
data class GameState(
    val healthPercent: Int = 100,
    val hungerPercent: Int = 100,
    val inventoryFull: Boolean = false,
    val enemyVisible: Boolean = false,
    val enemyPosition: Pair<Float, Float>? = null,
    val nearbyResource: String? = null, // e.g. "wood", "stone"
    val nearbyResourcePosition: Pair<Float, Float>? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

/** One detected object on screen, before being folded into GameState. */
data class Detection(
    val label: String,
    val confidence: Float,
    val boundingBox: android.graphics.RectF
)
