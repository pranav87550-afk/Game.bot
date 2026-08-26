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
                ActionExecutor.moveDirection(0.0) // walk toward it; refine with real bearing calc
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
        // TODO: aim at gameState.enemyPosition, attack
        if (!gameState.enemyVisible) return ExploreState()
        return this
    }
}

class FleeState : BotState {
    override fun tick(gameState: GameState): BotState {
        // TODO: move away from enemyPosition
        if (gameState.healthPercent > 60 || !gameState.enemyVisible) return ExploreState()
        return this
    }
}

class LootDropState : BotState {
    override fun tick(gameState: GameState): BotState {
        ActionExecutor.openInventory()
        // TODO: drop lowest-priority item(s)
        return if (!gameState.inventoryFull) ExploreState() else this
    }
}
