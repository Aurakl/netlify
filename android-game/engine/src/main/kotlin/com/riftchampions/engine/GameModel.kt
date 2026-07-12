package com.riftchampions.engine

/** Number of lanes on the board. Each side may have at most one creature per lane. */
const val LANE_COUNT = 3

/** A live creature on the board, distinct from its immutable [CardDef] template. */
data class CreatureInstance(
    val instanceId: Int,
    val def: CardDef,
    var currentAttack: Int,
    var currentHealth: Int,
    var maxHealth: Int,
    val keywords: MutableSet<Keyword>,
    var summoningSick: Boolean,
    var hasAttackedThisTurn: Boolean = false,
    var equippedWith: CardDef? = null,
)

data class PlayerState(
    val side: Side,
    val champion: ChampionDef,
    var championHealth: Int,
    val deck: MutableList<CardDef>,
    val hand: MutableList<CardDef>,
    val trash: MutableList<CardDef> = mutableListOf(),
    val lanes: Array<CreatureInstance?> = arrayOfNulls(LANE_COUNT),
    var resourcesInPlay: Int = 0,
    var resourcePlayedThisTurn: Boolean = false,
    var manaPool: Int = 0,
    var manaSpent: Int = 0,
    var championPowerUsedThisTurn: Boolean = false,
) {
    fun availableMana(): Int = manaPool - manaSpent
}

/** A reference to something an [Effect] can be aimed at. */
sealed class Target {
    data class Creature(val side: Side, val lane: Int) : Target()
    data class Champion(val side: Side) : Target()
}

sealed class ActionResult {
    data object Success : ActionResult()
    data class Failure(val reason: String) : ActionResult()
}

class GameState(
    val playerOne: PlayerState,
    val playerTwo: PlayerState,
    var currentTurn: Side = Side.PLAYER_ONE,
    var turnNumber: Int = 1,
    var winner: Side? = null,
) {
    var nextInstanceId: Int = 1
    val log: MutableList<String> = mutableListOf()

    fun player(side: Side): PlayerState = if (side == Side.PLAYER_ONE) playerOne else playerTwo
    fun opponentOf(side: Side): PlayerState = if (side == Side.PLAYER_ONE) playerTwo else playerOne
    fun current(): PlayerState = player(currentTurn)
    fun opponent(): PlayerState = opponentOf(currentTurn)
}
