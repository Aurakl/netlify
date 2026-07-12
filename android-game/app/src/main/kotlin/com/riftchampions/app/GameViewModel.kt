package com.riftchampions.app

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.riftchampions.engine.ActionResult
import com.riftchampions.engine.CardSet
import com.riftchampions.engine.CardType
import com.riftchampions.engine.Effect
import com.riftchampions.engine.Faction
import com.riftchampions.engine.GameEngine
import com.riftchampions.engine.Side
import com.riftchampions.engine.Target
import com.riftchampions.engine.ai.SimpleAi

/** A card selected from hand, waiting for the player to tap a lane to resolve it. */
data class PendingPlacement(val handIndex: Int)

/**
 * Drives a local game (single device): either against [SimpleAi] or two humans passing the
 * device back and forth. `engine.state` is plain mutable Kotlin, not Compose-observable on its
 * own, so [revision] is bumped after every mutation; composables read it once at the top of the
 * screen to subscribe, then read `engine.state` fresh on every recomposition it triggers.
 */
class GameViewModel(
    playerOneFaction: Faction,
    playerTwoFaction: Faction,
    val mode: GameMode,
) {
    val engine: GameEngine = GameEngine.newGame(
        playerOneChampion = CardSet.championFor(playerOneFaction),
        playerOneDeck = CardSet.buildPreconstructedDeck(playerOneFaction),
        playerTwoChampion = CardSet.championFor(playerTwoFaction),
        playerTwoDeck = CardSet.buildPreconstructedDeck(playerTwoFaction),
    )

    private val ai: SimpleAi? = if (mode == GameMode.VS_AI) SimpleAi(engine, Side.PLAYER_TWO) else null

    private val _revision = mutableIntStateOf(0)
    val revision: State<Int> get() = _revision

    private val _pending = mutableStateOf<PendingPlacement?>(null)
    val pending: State<PendingPlacement?> get() = _pending

    private val _selectedAttackerLane = mutableStateOf<Int?>(null)
    val selectedAttackerLane: State<Int?> get() = _selectedAttackerLane

    private val _message = mutableStateOf<String?>(null)
    val message: State<String?> get() = _message

    /** Whether the side whose turn it currently is should react to taps on this device right now. */
    fun isInteractive(): Boolean {
        if (engine.state.winner != null) return false
        return when (mode) {
            GameMode.VS_AI -> engine.state.currentTurn == Side.PLAYER_ONE
            GameMode.LOCAL_HOTSEAT -> true
            GameMode.ONLINE -> false // handled by OnlineGameViewModel instead
        }
    }

    fun onHandCardTapped(index: Int) {
        if (!isInteractive()) return
        val player = engine.state.current()
        val card = player.hand.getOrNull(index) ?: return
        _selectedAttackerLane.value = null

        when (card.type) {
            CardType.RESOURCE -> {
                report(engine.playResourceCard(index))
                _pending.value = null
            }
            CardType.CREATURE, CardType.EQUIPMENT -> {
                _pending.value = PendingPlacement(index)
            }
            CardType.SPELL -> {
                when (card.effect) {
                    is Effect.Buff -> _pending.value = PendingPlacement(index)
                    else -> {
                        report(engine.playSpell(index, autoTarget(card.effect, player.side)))
                        _pending.value = null
                    }
                }
            }
        }
        bump()
    }

    fun onOwnLaneTapped(lane: Int) {
        if (!isInteractive()) return
        val pending = _pending.value
        if (pending != null) {
            val player = engine.state.current()
            val card = player.hand.getOrNull(pending.handIndex)
            val result = when (card?.type) {
                CardType.CREATURE -> engine.playCreature(pending.handIndex, lane)
                CardType.EQUIPMENT -> engine.playEquipment(pending.handIndex, lane)
                CardType.SPELL -> engine.playSpell(pending.handIndex, Target.Creature(player.side, lane))
                else -> ActionResult.Failure("Nothing to place")
            }
            report(result)
            _pending.value = null
        } else {
            val player = engine.state.current()
            val creature = player.lanes.getOrNull(lane)
            _selectedAttackerLane.value = if (creature != null && !creature.summoningSick && !creature.hasAttackedThisTurn) {
                if (_selectedAttackerLane.value == lane) null else lane
            } else {
                null
            }
        }
        bump()
    }

    fun onEnemyLaneTapped(lane: Int) {
        if (!isInteractive()) return
        val attackerLane = _selectedAttackerLane.value ?: return
        report(engine.attack(attackerLane, lane))
        _selectedAttackerLane.value = null
        bump()
    }

    fun useChampionPower() {
        if (!isInteractive()) return
        val player = engine.state.current()
        report(engine.useChampionPower(autoTarget(player.champion.powerEffect, player.side)))
        bump()
    }

    fun cancelPending() {
        _pending.value = null
        _selectedAttackerLane.value = null
        bump()
    }

    fun endTurn() {
        if (engine.state.winner != null) return
        engine.endTurn()
        _pending.value = null
        _selectedAttackerLane.value = null
        _message.value = null
        bump()
        maybeRunAi()
    }

    private fun maybeRunAi() {
        if (mode == GameMode.VS_AI && engine.state.currentTurn == Side.PLAYER_TWO && engine.state.winner == null) {
            ai?.takeTurn()
            bump()
        }
    }

    private fun report(result: ActionResult) {
        _message.value = (result as? ActionResult.Failure)?.reason
    }

    private fun autoTarget(effect: Effect, casterSide: Side): Target? {
        val enemy = if (casterSide == Side.PLAYER_ONE) Side.PLAYER_TWO else Side.PLAYER_ONE
        return when (effect) {
            is Effect.Damage, is Effect.Drain -> Target.Champion(enemy)
            is Effect.Heal -> Target.Champion(casterSide)
            is Effect.Draw, is Effect.None -> null
            is Effect.Buff -> null
        }
    }

    private fun bump() {
        _revision.intValue++
    }
}
