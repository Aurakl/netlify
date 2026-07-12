package com.riftchampions.engine

import kotlin.random.Random

/**
 * Rules engine for Rift Champions, a 1v1 lane-based card battler.
 *
 * Board: each side has [LANE_COUNT] lanes, at most one creature per lane.
 * Mana: playing a Resource card (max one per turn) immediately grants +1 mana this
 * turn and permanently increases the mana available on future turns.
 * Combat is lane-locked: a creature attacks the enemy creature in the mirrored
 * lane, or the enemy champion directly if that lane is empty. RANGED creatures
 * may instead target any lane.
 */
class GameEngine(val state: GameState) {

    companion object {
        const val STARTING_HAND_SIZE = 4

        fun newGame(
            playerOneChampion: ChampionDef,
            playerOneDeck: List<CardDef>,
            playerTwoChampion: ChampionDef,
            playerTwoDeck: List<CardDef>,
            shuffle: Boolean = true,
            rng: Random = Random.Default,
        ): GameEngine {
            val p1Deck = playerOneDeck.toMutableList().also { if (shuffle) it.shuffle(rng) }
            val p2Deck = playerTwoDeck.toMutableList().also { if (shuffle) it.shuffle(rng) }

            val p1 = PlayerState(Side.PLAYER_ONE, playerOneChampion, playerOneChampion.maxHealth, p1Deck, mutableListOf())
            val p2 = PlayerState(Side.PLAYER_TWO, playerTwoChampion, playerTwoChampion.maxHealth, p2Deck, mutableListOf())

            repeat(STARTING_HAND_SIZE) {
                if (p1.deck.isNotEmpty()) p1.hand.add(p1.deck.removeAt(0))
                if (p2.deck.isNotEmpty()) p2.hand.add(p2.deck.removeAt(0))
            }

            val state = GameState(p1, p2, currentTurn = Side.PLAYER_ONE, turnNumber = 1)
            val engine = GameEngine(state)
            engine.beginTurn(isFirstTurnOfGame = true)
            return engine
        }
    }

    // ---- Turn management ---------------------------------------------------

    private fun beginTurn(isFirstTurnOfGame: Boolean = false) {
        val player = state.current()
        player.manaPool = player.resourcesInPlay
        player.manaSpent = 0
        player.resourcePlayedThisTurn = false
        player.championPowerUsedThisTurn = false
        for (creature in player.lanes) {
            creature?.summoningSick = false
            creature?.hasAttackedThisTurn = false
        }
        // The player who goes first skips their very first draw, to offset the advantage of acting first.
        if (!isFirstTurnOfGame || state.currentTurn != Side.PLAYER_ONE) {
            drawCard(player)
        }
    }

    private fun drawCard(player: PlayerState) {
        if (state.winner != null) return
        if (player.deck.isEmpty()) {
            state.winner = state.opponentOf(player.side).side
            state.log.add("${player.side} has no cards left to draw and loses.")
            return
        }
        player.hand.add(player.deck.removeAt(0))
    }

    fun endTurn(): ActionResult {
        if (state.winner != null) return ActionResult.Failure("Game already over")
        state.currentTurn = if (state.currentTurn == Side.PLAYER_ONE) Side.PLAYER_TWO else Side.PLAYER_ONE
        if (state.currentTurn == Side.PLAYER_ONE) state.turnNumber++
        beginTurn()
        return ActionResult.Success
    }

    // ---- Playing cards -------------------------------------------------------

    fun playResourceCard(handIndex: Int): ActionResult {
        val player = state.current()
        guardActive()?.let { return it }
        val card = player.hand.getOrNull(handIndex) ?: return ActionResult.Failure("No card at that hand index")
        if (card.type != CardType.RESOURCE) return ActionResult.Failure("${card.name} is not a resource card")
        if (player.resourcePlayedThisTurn) return ActionResult.Failure("Already played a resource card this turn")

        player.hand.removeAt(handIndex)
        player.resourcesInPlay++
        player.manaPool++
        player.resourcePlayedThisTurn = true
        state.log.add("${player.side} plays resource ${card.name}")
        return ActionResult.Success
    }

    fun playCreature(handIndex: Int, lane: Int): ActionResult {
        val player = state.current()
        guardActive()?.let { return it }
        val card = player.hand.getOrNull(handIndex) ?: return ActionResult.Failure("No card at that hand index")
        if (card.type != CardType.CREATURE) return ActionResult.Failure("${card.name} is not a creature")
        validLane(lane)?.let { return it }
        if (player.lanes[lane] != null) return ActionResult.Failure("Lane $lane is already occupied")
        if (player.availableMana() < card.cost) return ActionResult.Failure("Not enough mana")

        player.hand.removeAt(handIndex)
        player.manaSpent += card.cost
        player.lanes[lane] = CreatureInstance(
            instanceId = state.nextInstanceId++,
            def = card,
            currentAttack = card.attack,
            currentHealth = card.health,
            maxHealth = card.health,
            keywords = card.keywords.toMutableSet(),
            summoningSick = !card.keywords.contains(Keyword.CHARGE),
        )
        state.log.add("${player.side} plays ${card.name} in lane $lane")
        return ActionResult.Success
    }

    fun playEquipment(handIndex: Int, lane: Int): ActionResult {
        val player = state.current()
        guardActive()?.let { return it }
        val card = player.hand.getOrNull(handIndex) ?: return ActionResult.Failure("No card at that hand index")
        if (card.type != CardType.EQUIPMENT) return ActionResult.Failure("${card.name} is not equipment")
        validLane(lane)?.let { return it }
        val creature = player.lanes[lane] ?: return ActionResult.Failure("No creature in lane $lane to equip")
        if (player.availableMana() < card.cost) return ActionResult.Failure("Not enough mana")

        player.hand.removeAt(handIndex)
        player.manaSpent += card.cost
        creature.currentAttack += card.attack
        creature.maxHealth += card.health
        creature.currentHealth += card.health
        creature.keywords.addAll(card.keywords)
        creature.equippedWith = card
        state.log.add("${player.side} equips ${card.name} onto ${creature.def.name}")
        return ActionResult.Success
    }

    fun playSpell(handIndex: Int, target: Target? = null): ActionResult {
        val player = state.current()
        guardActive()?.let { return it }
        val card = player.hand.getOrNull(handIndex) ?: return ActionResult.Failure("No card at that hand index")
        if (card.type != CardType.SPELL) return ActionResult.Failure("${card.name} is not a spell")
        if (player.availableMana() < card.cost) return ActionResult.Failure("Not enough mana")

        val result = resolveEffect(card.effect, player, target)
        if (result is ActionResult.Failure) return result

        player.hand.removeAt(handIndex)
        player.manaSpent += card.cost
        player.trash.add(card)
        state.log.add("${player.side} casts ${card.name}")
        return ActionResult.Success
    }

    fun useChampionPower(target: Target? = null): ActionResult {
        val player = state.current()
        guardActive()?.let { return it }
        if (player.championPowerUsedThisTurn) return ActionResult.Failure("Champion power already used this turn")
        val cost = player.champion.powerCost
        if (player.availableMana() < cost) return ActionResult.Failure("Not enough mana")

        val result = resolveEffect(player.champion.powerEffect, player, target)
        if (result is ActionResult.Failure) return result

        player.manaSpent += cost
        player.championPowerUsedThisTurn = true
        state.log.add("${player.side} uses champion power: ${player.champion.name}")
        return ActionResult.Success
    }

    // ---- Combat ---------------------------------------------------------------

    /**
     * Attacks with the creature in [lane]. If that creature has RANGED, [chosenEnemyLane]
     * selects which enemy lane to hit; otherwise combat is locked to the mirrored lane.
     */
    fun attack(lane: Int, chosenEnemyLane: Int? = null): ActionResult {
        val player = state.current()
        guardActive()?.let { return it }
        validLane(lane)?.let { return it }
        val attacker = player.lanes[lane] ?: return ActionResult.Failure("No creature in lane $lane")
        if (attacker.summoningSick) return ActionResult.Failure("${attacker.def.name} has summoning sickness")
        if (attacker.hasAttackedThisTurn) return ActionResult.Failure("${attacker.def.name} already attacked this turn")

        val targetLane = if (attacker.keywords.contains(Keyword.RANGED) && chosenEnemyLane != null) {
            validLane(chosenEnemyLane)?.let { return it }
            chosenEnemyLane
        } else {
            lane
        }

        val opponent = state.opponent()
        val defender = opponent.lanes[targetLane]
        attacker.hasAttackedThisTurn = true

        if (defender == null) {
            opponent.championHealth -= attacker.currentAttack
            if (attacker.keywords.contains(Keyword.LIFESTEAL)) healChampion(player, attacker.currentAttack)
            state.log.add("${attacker.def.name} hits ${opponent.side}'s champion for ${attacker.currentAttack}")
            checkChampionDeath(opponent)
            return ActionResult.Success
        }

        defender.currentHealth -= attacker.currentAttack
        attacker.currentHealth -= defender.currentAttack
        if (attacker.keywords.contains(Keyword.LIFESTEAL)) healChampion(player, attacker.currentAttack)
        if (defender.currentHealth in 1..defender.maxHealth && attacker.keywords.contains(Keyword.POISON) && attacker.currentAttack > 0) {
            defender.currentHealth = 0
        }
        if (defender.keywords.contains(Keyword.LIFESTEAL) && attacker.currentHealth > 0) {
            healChampion(opponent, defender.currentAttack)
        }
        state.log.add("${attacker.def.name} trades blows with ${defender.def.name}")

        if (defender.currentHealth <= 0) {
            opponent.trash.add(defender.def)
            defender.equippedWith?.let { opponent.trash.add(it) }
            opponent.lanes[targetLane] = null
        }
        if (attacker.currentHealth <= 0) {
            player.trash.add(attacker.def)
            attacker.equippedWith?.let { player.trash.add(it) }
            player.lanes[lane] = null
        }
        return ActionResult.Success
    }

    private fun healChampion(player: PlayerState, amount: Int) {
        player.championHealth = (player.championHealth + amount).coerceAtMost(player.champion.maxHealth)
    }

    private fun checkChampionDeath(player: PlayerState) {
        if (player.championHealth <= 0 && state.winner == null) {
            state.winner = state.opponentOf(player.side).side
            state.log.add("${player.side}'s champion has fallen. ${state.winner} wins!")
        }
    }

    // ---- Effect resolution -----------------------------------------------------

    private fun resolveEffect(effect: Effect, caster: PlayerState, target: Target?): ActionResult {
        return when (effect) {
            is Effect.None -> ActionResult.Success
            is Effect.Draw -> {
                repeat(effect.count) { drawCard(caster) }
                ActionResult.Success
            }
            is Effect.Heal -> {
                when (target) {
                    is Target.Champion -> {
                        healChampion(state.player(target.side), effect.amount)
                        ActionResult.Success
                    }
                    is Target.Creature -> {
                        val creature = state.player(target.side).lanes[target.lane]
                            ?: return ActionResult.Failure("No creature at that target")
                        creature.currentHealth = (creature.currentHealth + effect.amount).coerceAtMost(creature.maxHealth)
                        ActionResult.Success
                    }
                    null -> ActionResult.Failure("This effect requires a target")
                }
            }
            is Effect.Damage -> applyDamage(effect.amount, target)
            is Effect.Drain -> {
                val result = applyDamage(effect.amount, target)
                if (result is ActionResult.Success) healChampion(caster, effect.amount)
                result
            }
            is Effect.Buff -> {
                val creatureTarget = target as? Target.Creature ?: return ActionResult.Failure("Buff requires a creature target")
                val creature = state.player(creatureTarget.side).lanes[creatureTarget.lane]
                    ?: return ActionResult.Failure("No creature at that target")
                creature.currentAttack += effect.attack
                creature.maxHealth += effect.health
                creature.currentHealth += effect.health
                ActionResult.Success
            }
        }
    }

    private fun applyDamage(amount: Int, target: Target?): ActionResult {
        return when (target) {
            is Target.Champion -> {
                val victim = state.player(target.side)
                victim.championHealth -= amount
                checkChampionDeath(victim)
                ActionResult.Success
            }
            is Target.Creature -> {
                val owner = state.player(target.side)
                val creature = owner.lanes[target.lane] ?: return ActionResult.Failure("No creature at that target")
                creature.currentHealth -= amount
                if (creature.currentHealth <= 0) {
                    owner.trash.add(creature.def)
                    creature.equippedWith?.let { owner.trash.add(it) }
                    owner.lanes[target.lane] = null
                }
                ActionResult.Success
            }
            null -> ActionResult.Failure("This effect requires a target")
        }
    }

    // ---- Guards -----------------------------------------------------------------

    private fun guardActive(): ActionResult.Failure? =
        if (state.winner != null) ActionResult.Failure("Game already over") else null

    private fun validLane(lane: Int): ActionResult.Failure? =
        if (lane !in 0 until LANE_COUNT) ActionResult.Failure("Invalid lane $lane") else null
}
