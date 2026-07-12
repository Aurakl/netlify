package com.riftchampions.engine

/**
 * The full original card pool for Rift Champions. Three fictional factions
 * (Ashen Vanguard, Verdant Wardens, Hollow Choir) plus a shared Neutral pool.
 * No relation to any existing trading card game's characters, art or text.
 */
object CardSet {

    // ---- Champions -------------------------------------------------------

    val ASHEN_CHAMPION = ChampionDef(
        id = "champ_ashen",
        name = "Kael, Ember Marshal",
        faction = Faction.ASHEN_VANGUARD,
        maxHealth = 20,
        powerCost = 2,
        powerEffect = Effect.Damage(2),
        powerDescription = "Deal 2 damage to a creature or the enemy champion.",
    )

    val VERDANT_CHAMPION = ChampionDef(
        id = "champ_verdant",
        name = "Branwen, Voice of the Grove",
        faction = Faction.VERDANT_WARDENS,
        maxHealth = 22,
        powerCost = 2,
        powerEffect = Effect.Heal(3),
        powerDescription = "Heal a friendly creature or your own champion for 3.",
    )

    val HOLLOW_CHAMPION = ChampionDef(
        id = "champ_hollow",
        name = "Mordecai, the Unbound",
        faction = Faction.HOLLOW_CHOIR,
        maxHealth = 20,
        powerCost = 3,
        powerEffect = Effect.Drain(2),
        powerDescription = "Deal 2 damage to the enemy champion and heal yours for 2.",
    )

    val ALL_CHAMPIONS = listOf(ASHEN_CHAMPION, VERDANT_CHAMPION, HOLLOW_CHAMPION)

    // ---- Ashen Vanguard (fire, aggressive) --------------------------------

    val ASHEN_RESOURCE = CardDef("ash_resource", "Ember Shard", Faction.ASHEN_VANGUARD, CardType.RESOURCE, cost = 0)

    private val ashenPool = listOf(
        CardDef("ash_1", "Vanguard Recruit", Faction.ASHEN_VANGUARD, CardType.CREATURE, cost = 1, attack = 2, health = 1),
        CardDef("ash_2", "Ember Wolf", Faction.ASHEN_VANGUARD, CardType.CREATURE, cost = 2, attack = 3, health = 1, keywords = setOf(Keyword.CHARGE)),
        CardDef("ash_3", "Ashen Archer", Faction.ASHEN_VANGUARD, CardType.CREATURE, cost = 3, attack = 2, health = 3, keywords = setOf(Keyword.RANGED)),
        CardDef("ash_4", "Bloodfang Raider", Faction.ASHEN_VANGUARD, CardType.CREATURE, cost = 3, attack = 3, health = 2, keywords = setOf(Keyword.LIFESTEAL)),
        CardDef("ash_5", "Venomtusk Boar", Faction.ASHEN_VANGUARD, CardType.CREATURE, cost = 3, attack = 2, health = 4, keywords = setOf(Keyword.POISON)),
        CardDef("ash_6", "Cinder Berserker", Faction.ASHEN_VANGUARD, CardType.CREATURE, cost = 4, attack = 5, health = 3),
        CardDef("ash_7", "Flame Colossus", Faction.ASHEN_VANGUARD, CardType.CREATURE, cost = 6, attack = 7, health = 6),
        CardDef("ash_8", "Firebolt", Faction.ASHEN_VANGUARD, CardType.SPELL, cost = 1, effect = Effect.Damage(2), description = "Deal 2 damage."),
        CardDef("ash_9", "Inferno Blast", Faction.ASHEN_VANGUARD, CardType.SPELL, cost = 4, effect = Effect.Damage(5), description = "Deal 5 damage."),
        CardDef("ash_10", "War Cry", Faction.ASHEN_VANGUARD, CardType.SPELL, cost = 2, effect = Effect.Buff(2, 1), description = "Give a friendly creature +2/+1."),
        CardDef("ash_11", "Blazing Gauntlet", Faction.ASHEN_VANGUARD, CardType.EQUIPMENT, cost = 2, attack = 2, health = 0, description = "Equip: +2/+0."),
        CardDef("ash_12", "Ridgeback Raider", Faction.ASHEN_VANGUARD, CardType.CREATURE, cost = 2, attack = 2, health = 2),
    )

    // ---- Verdant Wardens (nature, growth) --------------------------------

    val VERDANT_RESOURCE = CardDef("ver_resource", "Grovehart Seed", Faction.VERDANT_WARDENS, CardType.RESOURCE, cost = 0)

    private val verdantPool = listOf(
        CardDef("ver_1", "Sprout Sentinel", Faction.VERDANT_WARDENS, CardType.CREATURE, cost = 1, attack = 1, health = 3),
        CardDef("ver_2", "Wild Stag", Faction.VERDANT_WARDENS, CardType.CREATURE, cost = 2, attack = 2, health = 2, keywords = setOf(Keyword.CHARGE)),
        CardDef("ver_3", "Grove Archer", Faction.VERDANT_WARDENS, CardType.CREATURE, cost = 3, attack = 2, health = 3, keywords = setOf(Keyword.RANGED)),
        CardDef("ver_4", "Verdant Serpent", Faction.VERDANT_WARDENS, CardType.CREATURE, cost = 3, attack = 3, health = 3, keywords = setOf(Keyword.LIFESTEAL)),
        CardDef("ver_5", "Spore Widow", Faction.VERDANT_WARDENS, CardType.CREATURE, cost = 3, attack = 2, health = 3, keywords = setOf(Keyword.POISON)),
        CardDef("ver_6", "Regrowth Treant", Faction.VERDANT_WARDENS, CardType.CREATURE, cost = 4, attack = 3, health = 6),
        CardDef("ver_7", "Ancient Colossus", Faction.VERDANT_WARDENS, CardType.CREATURE, cost = 6, attack = 6, health = 8),
        CardDef("ver_8", "Healing Bloom", Faction.VERDANT_WARDENS, CardType.SPELL, cost = 2, effect = Effect.Heal(4), description = "Heal 4."),
        CardDef("ver_9", "Nature's Wrath", Faction.VERDANT_WARDENS, CardType.SPELL, cost = 3, effect = Effect.Damage(3), description = "Deal 3 damage."),
        CardDef("ver_10", "Wild Growth", Faction.VERDANT_WARDENS, CardType.SPELL, cost = 1, effect = Effect.Draw(1), description = "Draw a card."),
        CardDef("ver_11", "Barkhide Plating", Faction.VERDANT_WARDENS, CardType.EQUIPMENT, cost = 2, attack = 0, health = 3, description = "Equip: +0/+3."),
        CardDef("ver_12", "Thornback Turtle", Faction.VERDANT_WARDENS, CardType.CREATURE, cost = 2, attack = 1, health = 5),
    )

    // ---- Hollow Choir (undead, drain) -------------------------------------

    val HOLLOW_RESOURCE = CardDef("hol_resource", "Bone Shard", Faction.HOLLOW_CHOIR, CardType.RESOURCE, cost = 0)

    private val hollowPool = listOf(
        CardDef("hol_1", "Grave Shambler", Faction.HOLLOW_CHOIR, CardType.CREATURE, cost = 1, attack = 1, health = 2),
        CardDef("hol_2", "Bloodthorn Ghoul", Faction.HOLLOW_CHOIR, CardType.CREATURE, cost = 2, attack = 3, health = 1, keywords = setOf(Keyword.CHARGE)),
        CardDef("hol_3", "Wailing Wraith", Faction.HOLLOW_CHOIR, CardType.CREATURE, cost = 2, attack = 2, health = 1, keywords = setOf(Keyword.RANGED)),
        CardDef("hol_4", "Crimson Leech", Faction.HOLLOW_CHOIR, CardType.CREATURE, cost = 3, attack = 2, health = 2, keywords = setOf(Keyword.LIFESTEAL)),
        CardDef("hol_5", "Plague Bearer", Faction.HOLLOW_CHOIR, CardType.CREATURE, cost = 3, attack = 2, health = 3, keywords = setOf(Keyword.POISON)),
        CardDef("hol_6", "Bone Golem", Faction.HOLLOW_CHOIR, CardType.CREATURE, cost = 4, attack = 4, health = 5),
        CardDef("hol_7", "Soul Reaper", Faction.HOLLOW_CHOIR, CardType.CREATURE, cost = 5, attack = 5, health = 4, keywords = setOf(Keyword.LIFESTEAL)),
        CardDef("hol_8", "Dread Colossus", Faction.HOLLOW_CHOIR, CardType.CREATURE, cost = 6, attack = 7, health = 5),
        CardDef("hol_9", "Drain Essence", Faction.HOLLOW_CHOIR, CardType.SPELL, cost = 3, effect = Effect.Drain(3), description = "Deal 3 damage to the target and heal your champion for 3."),
        CardDef("hol_10", "Withering Curse", Faction.HOLLOW_CHOIR, CardType.SPELL, cost = 2, effect = Effect.Damage(2), description = "Deal 2 damage."),
        CardDef("hol_11", "Dark Pact", Faction.HOLLOW_CHOIR, CardType.SPELL, cost = 1, effect = Effect.Draw(2), description = "Draw 2 cards."),
        CardDef("hol_12", "Cursed Blade", Faction.HOLLOW_CHOIR, CardType.EQUIPMENT, cost = 2, attack = 2, health = 1, description = "Equip: +2/+1."),
    )

    // ---- Neutral (usable by any faction) ----------------------------------

    private val neutralPool = listOf(
        CardDef("neu_1", "Wandering Mercenary", Faction.NEUTRAL, CardType.CREATURE, cost = 2, attack = 2, health = 2),
        CardDef("neu_2", "Iron Militia", Faction.NEUTRAL, CardType.CREATURE, cost = 3, attack = 3, health = 3),
        CardDef("neu_3", "Scout Falcon", Faction.NEUTRAL, CardType.CREATURE, cost = 1, attack = 1, health = 1, keywords = setOf(Keyword.RANGED)),
        CardDef("neu_4", "Stone Guardian", Faction.NEUTRAL, CardType.CREATURE, cost = 4, attack = 2, health = 6),
        CardDef("neu_5", "Field Ration", Faction.NEUTRAL, CardType.SPELL, cost = 1, effect = Effect.Heal(2), description = "Heal 2."),
        CardDef("neu_6", "Quick Strike", Faction.NEUTRAL, CardType.SPELL, cost = 1, effect = Effect.Damage(1), description = "Deal 1 damage."),
        CardDef("neu_7", "Reinforced Shield", Faction.NEUTRAL, CardType.EQUIPMENT, cost = 1, attack = 0, health = 2, description = "Equip: +0/+2."),
        CardDef("neu_8", "Battle Standard", Faction.NEUTRAL, CardType.EQUIPMENT, cost = 3, attack = 2, health = 2, description = "Equip: +2/+2."),
    )

    val allCards: List<CardDef> by lazy {
        listOf(ASHEN_RESOURCE, VERDANT_RESOURCE, HOLLOW_RESOURCE) + ashenPool + verdantPool + hollowPool + neutralPool
    }

    private val byId: Map<String, CardDef> by lazy { allCards.associateBy { it.id } }

    /** Looks up a card definition by its stable id, e.g. for deserializing saved/networked state. */
    fun cardById(id: String): CardDef = byId[id] ?: error("Unknown card id: $id")

    fun poolFor(faction: Faction): List<CardDef> = when (faction) {
        Faction.ASHEN_VANGUARD -> ashenPool
        Faction.VERDANT_WARDENS -> verdantPool
        Faction.HOLLOW_CHOIR -> hollowPool
        Faction.NEUTRAL -> neutralPool
    }

    private fun resourceFor(faction: Faction): CardDef = when (faction) {
        Faction.ASHEN_VANGUARD -> ASHEN_RESOURCE
        Faction.VERDANT_WARDENS -> VERDANT_RESOURCE
        Faction.HOLLOW_CHOIR -> HOLLOW_RESOURCE
        Faction.NEUTRAL -> throw IllegalArgumentException("Neutral has no resource card")
    }

    fun championFor(faction: Faction): ChampionDef =
        ALL_CHAMPIONS.first { it.faction == faction }

    /**
     * Builds the fixed 30-card preconstructed deck for [faction]:
     * 10 resource cards + 1 copy of each of the 12 faction cards + 1 copy of each of the 8 neutral cards.
     * v1 ships with preconstructed decks only; a deckbuilder UI is a possible future addition.
     */
    fun buildPreconstructedDeck(faction: Faction): MutableList<CardDef> {
        val deck = mutableListOf<CardDef>()
        repeat(10) { deck.add(resourceFor(faction)) }
        deck.addAll(poolFor(faction))
        deck.addAll(neutralPool)
        check(deck.size == 30) { "Preconstructed deck must contain exactly 30 cards, got ${deck.size}" }
        return deck
    }
}
