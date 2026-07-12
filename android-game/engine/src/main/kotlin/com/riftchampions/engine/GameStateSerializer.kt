package com.riftchampions.engine

/**
 * Converts [GameState] to and from plain `Map<String, Any?>` / list / primitive structures,
 * the shape every document database (Firestore included) can store directly. Card templates
 * are referenced by their stable [CardDef.id] and re-resolved via [CardSet.cardById] on load,
 * so only live/mutable state (health, hand contents, board position, ...) is actually written.
 */
object GameStateSerializer {

    fun toMap(state: GameState): Map<String, Any?> = mapOf(
        "currentTurn" to state.currentTurn.name,
        "turnNumber" to state.turnNumber,
        "winner" to state.winner?.name,
        "nextInstanceId" to state.nextInstanceId,
        "playerOne" to playerToMap(state.playerOne),
        "playerTwo" to playerToMap(state.playerTwo),
    )

    fun fromMap(map: Map<String, Any?>): GameState {
        val p1 = playerFromMap(map["playerOne"] as Map<String, Any?>, Side.PLAYER_ONE)
        val p2 = playerFromMap(map["playerTwo"] as Map<String, Any?>, Side.PLAYER_TWO)
        val state = GameState(
            playerOne = p1,
            playerTwo = p2,
            currentTurn = Side.valueOf(map["currentTurn"] as String),
            turnNumber = (map["turnNumber"] as Number).toInt(),
            winner = (map["winner"] as String?)?.let { Side.valueOf(it) },
        )
        state.nextInstanceId = (map["nextInstanceId"] as Number).toInt()
        return state
    }

    private fun playerToMap(player: PlayerState): Map<String, Any?> = mapOf(
        "championId" to player.champion.id,
        "championHealth" to player.championHealth,
        "deck" to player.deck.map { it.id },
        "hand" to player.hand.map { it.id },
        "trash" to player.trash.map { it.id },
        "lanes" to player.lanes.map { it?.let { creature -> creatureToMap(creature) } },
        "resourcesInPlay" to player.resourcesInPlay,
        "resourcePlayedThisTurn" to player.resourcePlayedThisTurn,
        "manaPool" to player.manaPool,
        "manaSpent" to player.manaSpent,
        "championPowerUsedThisTurn" to player.championPowerUsedThisTurn,
    )

    private fun playerFromMap(map: Map<String, Any?>, side: Side): PlayerState {
        val champion = CardSet.ALL_CHAMPIONS.first { it.id == map["championId"] as String }
        @Suppress("UNCHECKED_CAST")
        val laneMaps = map["lanes"] as List<Map<String, Any?>?>
        val player = PlayerState(
            side = side,
            champion = champion,
            championHealth = (map["championHealth"] as Number).toInt(),
            deck = (map["deck"] as List<String>).map { CardSet.cardById(it) }.toMutableList(),
            hand = (map["hand"] as List<String>).map { CardSet.cardById(it) }.toMutableList(),
            trash = (map["trash"] as List<String>).map { CardSet.cardById(it) }.toMutableList(),
        )
        laneMaps.forEachIndexed { index, laneMap -> player.lanes[index] = laneMap?.let { creatureFromMap(it) } }
        player.resourcesInPlay = (map["resourcesInPlay"] as Number).toInt()
        player.resourcePlayedThisTurn = map["resourcePlayedThisTurn"] as Boolean
        player.manaPool = (map["manaPool"] as Number).toInt()
        player.manaSpent = (map["manaSpent"] as Number).toInt()
        player.championPowerUsedThisTurn = map["championPowerUsedThisTurn"] as Boolean
        return player
    }

    private fun creatureToMap(creature: CreatureInstance): Map<String, Any?> = mapOf(
        "instanceId" to creature.instanceId,
        "defId" to creature.def.id,
        "currentAttack" to creature.currentAttack,
        "currentHealth" to creature.currentHealth,
        "maxHealth" to creature.maxHealth,
        "keywords" to creature.keywords.map { it.name },
        "summoningSick" to creature.summoningSick,
        "hasAttackedThisTurn" to creature.hasAttackedThisTurn,
        "equippedWith" to creature.equippedWith?.id,
    )

    private fun creatureFromMap(map: Map<String, Any?>): CreatureInstance {
        @Suppress("UNCHECKED_CAST")
        val keywords = (map["keywords"] as List<String>).map { Keyword.valueOf(it) }.toMutableSet()
        return CreatureInstance(
            instanceId = (map["instanceId"] as Number).toInt(),
            def = CardSet.cardById(map["defId"] as String),
            currentAttack = (map["currentAttack"] as Number).toInt(),
            currentHealth = (map["currentHealth"] as Number).toInt(),
            maxHealth = (map["maxHealth"] as Number).toInt(),
            keywords = keywords,
            summoningSick = map["summoningSick"] as Boolean,
            hasAttackedThisTurn = map["hasAttackedThisTurn"] as Boolean,
            equippedWith = (map["equippedWith"] as String?)?.let { CardSet.cardById(it) },
        )
    }
}
