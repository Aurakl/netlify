package com.riftchampions.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val TEST_CHAMPION_A = ChampionDef("t_champ_a", "Test Champion A", Faction.ASHEN_VANGUARD, maxHealth = 20, powerCost = 2, powerEffect = Effect.Damage(2), powerDescription = "")
private val TEST_CHAMPION_B = ChampionDef("t_champ_b", "Test Champion B", Faction.VERDANT_WARDENS, maxHealth = 20, powerCost = 2, powerEffect = Effect.Heal(3), powerDescription = "")

private val VANILLA_1_2 = CardDef("t_creature_1_2", "Vanilla 1/2", Faction.NEUTRAL, CardType.CREATURE, cost = 1, attack = 1, health = 2)
private val VANILLA_3_3 = CardDef("t_creature_3_3", "Vanilla 3/3", Faction.NEUTRAL, CardType.CREATURE, cost = 2, attack = 3, health = 3)
private val CHARGER = CardDef("t_charger", "Test Charger", Faction.NEUTRAL, CardType.CREATURE, cost = 1, attack = 2, health = 1, keywords = setOf(Keyword.CHARGE))
private val RANGED_UNIT = CardDef("t_ranged", "Test Ranged", Faction.NEUTRAL, CardType.CREATURE, cost = 1, attack = 2, health = 1, keywords = setOf(Keyword.RANGED, Keyword.CHARGE))
private val LIFESTEAL_UNIT = CardDef("t_lifesteal", "Test Lifesteal", Faction.NEUTRAL, CardType.CREATURE, cost = 1, attack = 3, health = 1, keywords = setOf(Keyword.LIFESTEAL, Keyword.CHARGE))
private val POISON_UNIT = CardDef("t_poison", "Test Poison", Faction.NEUTRAL, CardType.CREATURE, cost = 1, attack = 1, health = 1, keywords = setOf(Keyword.POISON, Keyword.CHARGE))
private val RESOURCE_CARD = CardDef("t_resource", "Test Resource", Faction.NEUTRAL, CardType.RESOURCE, cost = 0)
private val DAMAGE_SPELL = CardDef("t_damage", "Test Damage", Faction.NEUTRAL, CardType.SPELL, cost = 1, effect = Effect.Damage(3))
private val HEAL_SPELL = CardDef("t_heal", "Test Heal", Faction.NEUTRAL, CardType.SPELL, cost = 1, effect = Effect.Heal(3))
private val DRAW_SPELL = CardDef("t_draw", "Test Draw", Faction.NEUTRAL, CardType.SPELL, cost = 0, effect = Effect.Draw(2))
private val BUFF_SPELL = CardDef("t_buff", "Test Buff", Faction.NEUTRAL, CardType.SPELL, cost = 1, effect = Effect.Buff(2, 2))
private val EQUIPMENT_CARD = CardDef("t_equip", "Test Equipment", Faction.NEUTRAL, CardType.EQUIPMENT, cost = 1, attack = 2, health = 1)

class GameEngineTest {

    private fun freshPlayer(side: Side, champion: ChampionDef, hand: List<CardDef> = emptyList(), deck: List<CardDef> = emptyList()): PlayerState =
        PlayerState(side, champion, champion.maxHealth, deck.toMutableList(), hand.toMutableList())

    private fun engineWith(p1: PlayerState, p2: PlayerState, turn: Side = Side.PLAYER_ONE): GameEngine =
        GameEngine(GameState(p1, p2, currentTurn = turn))

    private fun withMana(player: PlayerState, amount: Int): PlayerState {
        player.resourcesInPlay = amount
        player.manaPool = amount
        return player
    }

    @Test
    fun `playing a resource card grants mana immediately and is limited to one per turn`() {
        val p1 = freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(RESOURCE_CARD, RESOURCE_CARD))
        val p2 = freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B)
        val engine = engineWith(p1, p2)

        assertEquals(0, p1.availableMana())
        assertIs<ActionResult.Success>(engine.playResourceCard(0))
        assertEquals(1, p1.availableMana())
        assertEquals(1, p1.resourcesInPlay)

        val second = engine.playResourceCard(0)
        assertIs<ActionResult.Failure>(second)
        assertEquals(1, p1.availableMana())
    }

    @Test
    fun `playing a creature spends mana and occupies the chosen lane`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(VANILLA_3_3)), 2)
        val p2 = freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B)
        val engine = engineWith(p1, p2)

        assertIs<ActionResult.Success>(engine.playCreature(0, 1))
        assertEquals(0, p1.availableMana())
        assertEquals("Vanilla 3/3", p1.lanes[1]?.def?.name)
        assertTrue(p1.hand.isEmpty())

        val secondAttempt = engine.playCreature(0, 0)
        assertIs<ActionResult.Failure>(secondAttempt)
    }

    @Test
    fun `cannot play a creature into an occupied lane or without enough mana`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(VANILLA_1_2, VANILLA_3_3)), 1)
        val p2 = freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B)
        val engine = engineWith(p1, p2)

        assertIs<ActionResult.Success>(engine.playCreature(0, 0))
        assertIs<ActionResult.Failure>(engine.playCreature(0, 0))
        assertIs<ActionResult.Failure>(engine.playCreature(0, 1))
    }

    @Test
    fun `a freshly played creature has summoning sickness and cannot attack unless it has charge`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(VANILLA_1_2, CHARGER)), 5)
        val p2 = freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B)
        val engine = engineWith(p1, p2)

        engine.playCreature(0, 0)
        assertIs<ActionResult.Failure>(engine.attack(0))

        engine.playCreature(0, 1)
        assertIs<ActionResult.Success>(engine.attack(1))
        assertEquals(18, p2.championHealth)
    }

    @Test
    fun `attacking into an empty enemy lane damages the enemy champion`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(CHARGER)), 5)
        val p2 = freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B)
        val engine = engineWith(p1, p2)

        engine.playCreature(0, 2)
        engine.attack(2)
        assertEquals(18, p2.championHealth)
    }

    @Test
    fun `two creatures in combat trade damage and the weaker one dies`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(VANILLA_3_3), deck = List(3) { VANILLA_1_2 }), 5)
        val p2 = withMana(freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B, hand = listOf(VANILLA_1_2), deck = List(3) { VANILLA_1_2 }), 5)
        val engine = engineWith(p1, p2)
        engine.playCreature(0, 0)
        engine.endTurn()
        engine.playCreature(0, 0)
        engine.endTurn() // back to player one; their creature lost summoning sickness at the start of this turn

        assertIs<ActionResult.Success>(engine.attack(0))
        assertNull(p2.lanes[0], "defender (1/2) should die to 3 damage")
        assertEquals(2, p1.lanes[0]?.currentHealth, "attacker (3/3) should survive with 2 health after taking 1 damage")
    }

    @Test
    fun `lifesteal heals the attacker champion by damage dealt`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(LIFESTEAL_UNIT)), 5)
        p1.championHealth = 10
        val p2 = freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B)
        val engine = engineWith(p1, p2)

        engine.playCreature(0, 0)
        engine.attack(0)
        assertEquals(13, p1.championHealth)
        assertEquals(17, p2.championHealth)
    }

    @Test
    fun `poison destroys the defending creature outright regardless of remaining health`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(POISON_UNIT), deck = List(3) { VANILLA_1_2 }), 5)
        val tank = CardDef("t_tank", "Tank", Faction.NEUTRAL, CardType.CREATURE, cost = 1, attack = 0, health = 20)
        val p2 = withMana(freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B, hand = listOf(tank), deck = List(3) { VANILLA_1_2 }), 5)
        val engine = engineWith(p1, p2)

        engine.playCreature(0, 0)
        engine.endTurn()
        engine.playCreature(0, 0)
        engine.endTurn()

        engine.attack(0)
        assertNull(p2.lanes[0], "the 20-health tank should be destroyed outright by poison")
    }

    @Test
    fun `ranged creatures may choose which enemy lane to strike`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(RANGED_UNIT), deck = List(3) { VANILLA_1_2 }), 5)
        val blocker = CardDef("t_blocker", "Blocker", Faction.NEUTRAL, CardType.CREATURE, cost = 1, attack = 1, health = 5)
        val p2 = withMana(freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B, hand = listOf(blocker), deck = List(3) { VANILLA_1_2 }), 5)
        val engine = engineWith(p1, p2)

        engine.playCreature(0, 0) // ranged unit in lane 0
        engine.endTurn()
        engine.playCreature(0, 1) // blocker in lane 1 (mirrors lane 1, not lane 0)
        engine.endTurn()

        // lane 0 attacker chooses to strike lane 1 despite living in lane 0
        assertIs<ActionResult.Success>(engine.attack(0, chosenEnemyLane = 1))
        assertTrue(p2.lanes[1]!!.currentHealth < 5)
    }

    @Test
    fun `champion power can only be used once per turn and respects mana cost`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A), 2)
        val p2 = freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B)
        val engine = engineWith(p1, p2)

        assertIs<ActionResult.Success>(engine.useChampionPower(Target.Champion(Side.PLAYER_TWO)))
        assertEquals(18, p2.championHealth)
        assertIs<ActionResult.Failure>(engine.useChampionPower(Target.Champion(Side.PLAYER_TWO)))
    }

    @Test
    fun `spells deal damage and heal as expected`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(DAMAGE_SPELL, HEAL_SPELL)), 5)
        p1.championHealth = 10
        val p2 = freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B)
        val engine = engineWith(p1, p2)

        assertIs<ActionResult.Success>(engine.playSpell(0, Target.Champion(Side.PLAYER_TWO)))
        assertEquals(17, p2.championHealth)

        assertIs<ActionResult.Success>(engine.playSpell(0, Target.Champion(Side.PLAYER_ONE)))
        assertEquals(13, p1.championHealth)
    }

    @Test
    fun `draw spell draws cards and equipment buffs the target creature`() {
        val p1 = withMana(
            freshPlayer(
                Side.PLAYER_ONE, TEST_CHAMPION_A,
                hand = listOf(DRAW_SPELL, VANILLA_1_2, EQUIPMENT_CARD),
                deck = listOf(VANILLA_3_3, VANILLA_3_3),
            ),
            5,
        )
        val p2 = freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B)
        val engine = engineWith(p1, p2)

        assertIs<ActionResult.Success>(engine.playSpell(0, null))
        assertEquals(0, p1.deck.size)
        assertEquals(4, p1.hand.size) // started with 3, drew 2, played 1 spell => 3 - 1 + 2 = 4

        engine.playCreature(0, 0)
        engine.playEquipment(0, 0)
        assertEquals(3, p1.lanes[0]!!.currentAttack)
        assertEquals(3, p1.lanes[0]!!.currentHealth)
    }

    @Test
    fun `buff spell requires a friendly creature target`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(BUFF_SPELL)), 5)
        val p2 = freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B)
        val engine = engineWith(p1, p2)

        assertIs<ActionResult.Failure>(engine.playSpell(0, null))
    }

    @Test
    fun `drawing from an empty deck causes an immediate loss`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(RESOURCE_CARD, RESOURCE_CARD)), 5)
        val p2 = freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B, deck = emptyList())
        val engine = engineWith(p1, p2)

        engine.playResourceCard(0)
        engine.endTurn() // player two's turn begins, tries to draw from empty deck
        assertEquals(Side.PLAYER_ONE, engine.state.winner)
    }

    @Test
    fun `champion health reaching zero ends the game`() {
        val p1 = withMana(freshPlayer(Side.PLAYER_ONE, TEST_CHAMPION_A, hand = listOf(DAMAGE_SPELL)), 5)
        val p2 = freshPlayer(Side.PLAYER_TWO, TEST_CHAMPION_B)
        p2.championHealth = 3
        val engine = engineWith(p1, p2)

        engine.playSpell(0, Target.Champion(Side.PLAYER_TWO))
        assertEquals(Side.PLAYER_ONE, engine.state.winner)
    }

    @Test
    fun `a full AI vs AI game runs to completion without crashing`() {
        val p1 = freshPlayer(
            Side.PLAYER_ONE, CardSet.championFor(Faction.ASHEN_VANGUARD),
            deck = CardSet.buildPreconstructedDeck(Faction.ASHEN_VANGUARD),
        )
        val p2 = freshPlayer(
            Side.PLAYER_TWO, CardSet.championFor(Faction.HOLLOW_CHOIR),
            deck = CardSet.buildPreconstructedDeck(Faction.HOLLOW_CHOIR),
        )
        val engine = GameEngine.newGame(
            p1.champion, p1.deck, p2.champion, p2.deck,
            shuffle = true, rng = kotlin.random.Random(42),
        )
        val aiOne = com.riftchampions.engine.ai.SimpleAi(engine, Side.PLAYER_ONE)
        val aiTwo = com.riftchampions.engine.ai.SimpleAi(engine, Side.PLAYER_TWO)

        var safety = 0
        while (engine.state.winner == null && safety < 500) {
            if (engine.state.currentTurn == Side.PLAYER_ONE) aiOne.takeTurn() else aiTwo.takeTurn()
            safety++
        }

        assertTrue(engine.state.winner != null, "game should reach a winner within a reasonable number of turns")
    }

    @Test
    fun `game state survives a serialize-deserialize round trip`() {
        val p1 = freshPlayer(
            Side.PLAYER_ONE, CardSet.championFor(Faction.ASHEN_VANGUARD),
            deck = CardSet.buildPreconstructedDeck(Faction.ASHEN_VANGUARD),
        )
        val p2 = freshPlayer(
            Side.PLAYER_TWO, CardSet.championFor(Faction.VERDANT_WARDENS),
            deck = CardSet.buildPreconstructedDeck(Faction.VERDANT_WARDENS),
        )
        val engine = GameEngine.newGame(p1.champion, p1.deck, p2.champion, p2.deck, rng = kotlin.random.Random(7))
        val ai = com.riftchampions.engine.ai.SimpleAi(engine, Side.PLAYER_ONE)
        ai.takeTurn() // mutate the state a bit so the round trip isn't just checking the initial snapshot

        val restored = GameStateSerializer.fromMap(GameStateSerializer.toMap(engine.state))

        assertEquals(engine.state.currentTurn, restored.currentTurn)
        assertEquals(engine.state.turnNumber, restored.turnNumber)
        assertEquals(engine.state.playerOne.championHealth, restored.playerOne.championHealth)
        assertEquals(engine.state.playerOne.hand.map { it.id }, restored.playerOne.hand.map { it.id })
        assertEquals(engine.state.playerOne.deck.map { it.id }, restored.playerOne.deck.map { it.id })
        assertEquals(
            engine.state.playerOne.lanes.map { it?.def?.id },
            restored.playerOne.lanes.map { it?.def?.id },
        )
        assertEquals(
            engine.state.playerOne.lanes.map { it?.currentHealth },
            restored.playerOne.lanes.map { it?.currentHealth },
        )
        assertEquals(engine.state.playerTwo.championHealth, restored.playerTwo.championHealth)
    }
}
