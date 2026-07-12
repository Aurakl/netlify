package com.riftchampions.engine

/** Which side of the board a player occupies. */
enum class Side { PLAYER_ONE, PLAYER_TWO }

enum class Faction { NEUTRAL, ASHEN_VANGUARD, VERDANT_WARDENS, HOLLOW_CHOIR }

enum class CardType { CREATURE, SPELL, EQUIPMENT, RESOURCE }

/**
 * CHARGE: can attack the turn it enters play (ignores summoning sickness).
 * RANGED: may attack any enemy lane, not just the mirrored one.
 * LIFESTEAL: damage dealt while attacking also heals the attacker's champion.
 * POISON: any creature it damages in combat is destroyed outright, regardless of remaining health.
 */
enum class Keyword { CHARGE, RANGED, LIFESTEAL, POISON }

/** A one-shot effect resolved by a spell or a champion power. */
sealed class Effect {
    data class Damage(val amount: Int) : Effect()
    data class Heal(val amount: Int) : Effect()
    /** Damages the target and heals the caster's own champion by the same amount. */
    data class Drain(val amount: Int) : Effect()
    data class Buff(val attack: Int, val health: Int) : Effect()
    data class Draw(val count: Int) : Effect()
    data object None : Effect()
}

/** Static definition (template) of a card. Immutable — instances in play copy from this. */
data class CardDef(
    val id: String,
    val name: String,
    val faction: Faction,
    val type: CardType,
    val cost: Int,
    val attack: Int = 0,
    val health: Int = 0,
    val keywords: Set<Keyword> = emptySet(),
    val effect: Effect = Effect.None,
    val description: String = "",
)

/** Static definition of a champion (hero). Every deck is led by exactly one. */
data class ChampionDef(
    val id: String,
    val name: String,
    val faction: Faction,
    val maxHealth: Int,
    val powerCost: Int,
    val powerEffect: Effect,
    val powerDescription: String,
)
