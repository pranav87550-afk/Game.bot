package com.pranav.gamebot.decision.states

import com.pranav.gamebot.action.ActionExecutor
import com.pranav.gamebot.decision.BotState
import com.pranav.gamebot.perception.GameState

/** Default state: wander/move forward, collect nearby resources, and watch for threats. */
class ExploreState : BotState {
    override fun tick(gameState: GameState): BotState {
        return when {
            gameState.enemyVisible -> {
                if (gameState.healthPercent < 30) FleeState() else FightState()
            }
            gameState.inventoryFull -> LootDropState()
            gameState.nearbyResource != null -> {
                ActionExecutor.moveDirection(0.0)
                this
            }
            else -> {
                ActionExecutor.pressRun()
                ActionExecutor.moveDirection(0.0)
                this
            }
        }
    }
}

/** Placeholder states — flesh these out next. */
class FightState : BotState {
    override fun tick(gameState: GameState): BotState {
        if (!gameState.enemyVisible) return ExploreState()
        return this
    }
}

class FleeState : BotState {
    override fun tick(gameState: GameState): BotState {
        if (gameState.healthPercent > 60 || !gameState.enemyVisible) return ExploreState()
        return this
    }
}

class LootDropState : BotState {
    override fun tick(gameState: GameState): BotState {
        ActionExecutor.openInventory()
        return if (!gameState.inventoryFull) ExploreState() else this
    }
}
