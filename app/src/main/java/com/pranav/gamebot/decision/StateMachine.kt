package com.pranav.gamebot.decision

import com.pranav.gamebot.decision.states.ExploreState
import com.pranav.gamebot.perception.GameState

class StateMachine(private var current: BotState = ExploreState()) {

    val currentStateName: String get() = current::class.simpleName ?: "Unknown"

    fun tick(gameState: GameState) {
        current = current.tick(gameState)
    }
}
