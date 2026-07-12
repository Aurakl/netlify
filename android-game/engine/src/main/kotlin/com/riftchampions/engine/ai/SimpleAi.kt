package com.riftchampions.engine.ai

import com.riftchampions.engine.ActionResult
import com.riftchampions.engine.CardType
import com.riftchampions.engine.Effect
import com.riftchampions.engine.GameEngine
import com.riftchampions.engine.Keyword
import com.riftchampions.engine.LANE_COUNT
import com.riftchampions.engine.Side
import com.riftchampions.engine.Target

/**
 * A greedy, rule-based opponent for single-player games. Not meant to be optimal —
 * just competent enough to be a reasonable practice partner. Each turn it plays a
 * resource, dumps as many affordable creatures as lanes allow, fires off spells and
 * its champion power with simple heuristics, then attacks with everything able to.
 */
class SimpleAi(private val engine: GameEngine, private val side: Side) {

    private val enemySide: Side get() = if (side == Side.PLAYER_ONE) Side.PLAYER_TWO else Side.PLAYER_ONE

    private sealed class TargetResolution {
        data object NotNeeded : TargetResolution()
        data class Found(val target: Target) : TargetResolution()
        data object NoValidTarget : TargetResolution()
    }

    fun takeTurn() {
        if (engine.state.currentTurn != side || engine.state.winner != null) return

        playResource()
        playCreatures()
        playEquipment()
        playSpells()
        useChampionPower()
        attackWithEveryone()

        if (engine.state.currentTurn == side && engine.state.winner == null) {
            engine.endTurn()
        }
    }

    private fun playResource() {
        val player = engine.state.player(side)
        if (player.resourcePlayedThisTurn) return
        val index = player.hand.indexOfFirst { it.type == CardType.RESOURCE }
        if (index >= 0) engine.playResourceCard(index)
    }

    private fun playCreatures() {
        while (true) {
            val player = engine.state.player(side)
            val emptyLane = player.lanes.indexOfFirst { it == null }
            if (emptyLane == -1) return
            val handIndex = player.hand.withIndex()
                .filter { (_, card) -> card.type == CardType.CREATURE && card.cost <= player.availableMana() }
                .maxByOrNull { (_, card) -> card.cost }
                ?.index ?: return
            if (engine.playCreature(handIndex, emptyLane) !is ActionResult.Success) return
        }
    }

    private fun playEquipment() {
        while (true) {
            val player = engine.state.player(side)
            val bestLane = player.lanes.withIndex().filter { it.value != null }
                .maxByOrNull { it.value!!.currentAttack }?.index ?: return
            val handIndex = player.hand.indexOfFirst {
                it.type == CardType.EQUIPMENT && it.cost <= player.availableMana()
            }
            if (handIndex == -1) return
            if (engine.playEquipment(handIndex, bestLane) !is ActionResult.Success) return
        }
    }

    private fun playSpells() {
        while (true) {
            val player = engine.state.player(side)
            var acted = false
            for ((index, card) in player.hand.withIndex()) {
                if (card.type != CardType.SPELL || card.cost > player.availableMana()) continue
                when (val resolution = resolveTarget(card.effect)) {
                    is TargetResolution.NoValidTarget -> continue
                    is TargetResolution.NotNeeded -> {
                        if (engine.playSpell(index, null) is ActionResult.Success) acted = true
                    }
                    is TargetResolution.Found -> {
                        if (engine.playSpell(index, resolution.target) is ActionResult.Success) acted = true
                    }
                }
                if (acted) break
            }
            if (!acted) return
        }
    }

    private fun useChampionPower() {
        val player = engine.state.player(side)
        if (player.championPowerUsedThisTurn) return
        if (player.availableMana() < player.champion.powerCost) return
        when (val resolution = resolveTarget(player.champion.powerEffect)) {
            is TargetResolution.NoValidTarget -> return
            is TargetResolution.NotNeeded -> engine.useChampionPower(null)
            is TargetResolution.Found -> engine.useChampionPower(resolution.target)
        }
    }

    private fun resolveTarget(effect: Effect): TargetResolution {
        val player = engine.state.player(side)
        return when (effect) {
            is Effect.Damage, is Effect.Drain -> TargetResolution.Found(Target.Champion(enemySide))
            is Effect.Heal -> TargetResolution.Found(Target.Champion(side))
            is Effect.Draw -> TargetResolution.NotNeeded
            is Effect.Buff -> {
                val lane = player.lanes.indexOfFirst { it != null }
                if (lane == -1) TargetResolution.NoValidTarget else TargetResolution.Found(Target.Creature(side, lane))
            }
            is Effect.None -> TargetResolution.NotNeeded
        }
    }

    private fun attackWithEveryone() {
        val player = engine.state.player(side)
        for (lane in 0 until LANE_COUNT) {
            val creature = player.lanes[lane] ?: continue
            if (creature.summoningSick || creature.hasAttackedThisTurn) continue
            if (creature.keywords.contains(Keyword.RANGED)) {
                val enemy = engine.state.player(enemySide)
                val openLane = (0 until LANE_COUNT).firstOrNull { enemy.lanes[it] == null }
                engine.attack(lane, openLane)
            } else {
                engine.attack(lane)
            }
        }
    }
}
