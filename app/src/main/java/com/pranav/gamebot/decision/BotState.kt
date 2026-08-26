package com.pranav.gamebot.decision

import com.pranav.gamebot.perception.GameState

/** Every concrete bot behavior state implements this. */
interface BotState {
    /** Decide what to do this tick given current perception. Returns the next state to transition to. */
    fun tick(gameState: GameState): BotState
}
