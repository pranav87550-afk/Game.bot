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
                val resourcePos = gameState.nearbyResourcePosition
                if (resourcePos != null) {
                    ActionExecutor.moveDirection(ActionExecutor.bearingTo(resourcePos))
                }
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

/**
 * Engage a visible enemy: attack its on-screen position each tick, bail out
 * to FleeState if health drops too low mid-fight, or back to ExploreState
 * once the enemy is gone (dead or out of view).
 */
class FightState : BotState {
    companion object {
        private const val FLEE_HEALTH_THRESHOLD = 30
    }

    override fun tick(gameState: GameState): BotState {
        if (!gameState.enemyVisible) return ExploreState()
        if (gameState.healthPercent < FLEE_HEALTH_THRESHOLD) return FleeState()

        gameState.enemyPosition?.let { ActionExecutor.attack(it) }
        return this
    }
}

/**
 * Run away from the last known enemy position. Keeps moving each tick until
 * either health has recovered enough to fight, or the enemy is no longer
 * visible (escaped) — at which point resume exploring.
 */
class FleeState : BotState {
    companion object {
        private const val SAFE_HEALTH_THRESHOLD = 60
    }

    // Remember where the threat was even after it drops out of view for a tick,
    // so we don't immediately stop moving the instant detection flickers.
    private var lastKnownEnemyPosition: Pair<Float, Float>? = null

    override fun tick(gameState: GameState): BotState {
        gameState.enemyPosition?.let { lastKnownEnemyPosition = it }

        if (gameState.healthPercent >= SAFE_HEALTH_THRESHOLD || !gameState.enemyVisible) {
            return ExploreState()
        }

        lastKnownEnemyPosition?.let { threat ->
            ActionExecutor.pressRun()
            ActionExecutor.moveDirection(ActionExecutor.bearingAwayFrom(threat))
        }
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
