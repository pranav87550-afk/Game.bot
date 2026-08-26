package com.pranav.gamebot.perception

/**
 * Structured snapshot of "what the bot currently sees", built from raw
 * detections each frame. Decision layer reads ONLY this object.
 */
data class GameState(
    val healthPercent: Int = 100,
    val hungerPercent: Int = 100,
    val inventoryFull: Boolean = false,
    val enemyVisible: Boolean = false,
    val enemyPosition: Pair<Float, Float>? = null,
    val nearbyResource: String? = null,
    val nearbyResourcePosition: Pair<Float, Float>? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

data class Detection(
    val label: String,
    val confidence: Float,
    val boundingBox: android.graphics.RectF
)
