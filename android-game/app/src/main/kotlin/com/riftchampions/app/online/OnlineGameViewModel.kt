package com.riftchampions.app.online

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.firestore.ListenerRegistration
import com.riftchampions.app.PendingPlacement
import com.riftchampions.engine.ActionResult
import com.riftchampions.engine.CardSet
import com.riftchampions.engine.CardType
import com.riftchampions.engine.Effect
import com.riftchampions.engine.Faction
import com.riftchampions.engine.GameEngine
import com.riftchampions.engine.GameStateSerializer
import com.riftchampions.engine.Side
import com.riftchampions.engine.Target

/**
 * Drives one online match. The whole [GameEngine]/[com.riftchampions.engine.GameState] is
 * re-serialized and overwritten in Firestore after every move (see [FirebaseOnlineRepository]) —
 * simple and robust enough for a turn-based 1v1 game, at the cost of a bit more bandwidth than
 * a real operation log would use.
 *
 * The host (PLAYER_ONE) is responsible for creating the actual game once a guest has joined;
 * the guest just waits for the first state document to appear.
 */
class OnlineGameViewModel(
    val roomCode: String,
    val mySide: Side,
    private val repository: FirebaseOnlineRepository,
) {
    private val _engine = mutableStateOf<GameEngine?>(null)
    val engineState: State<GameEngine?> get() = _engine

    private val _waitingMessage = mutableStateOf("Waiting for opponent to join... Room code: $roomCode")
    val waitingMessage: State<String> get() = _waitingMessage

    private val _message = mutableStateOf<String?>(null)
    val message: State<String?> get() = _message

    private val _pending = mutableStateOf<PendingPlacement?>(null)
    val pending: State<PendingPlacement?> get() = _pending

    private val _selectedAttackerLane = mutableStateOf<Int?>(null)
    val selectedAttackerLane: State<Int?> get() = _selectedAttackerLane

    private var registration: ListenerRegistration? = null
    private var gameInitialized = false

    fun start() {
        registration = repository.listen(roomCode) { data -> handleUpdate(data) }
    }

    fun dispose() {
        registration?.remove()
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleUpdate(data: Map<String, Any?>?) {
        if (data == null) return
        val hostFactionName = data["hostFaction"] as? String
        val guestFactionName = data["guestFaction"] as? String
        val stateMap = data["state"] as? Map<String, Any?>

        if (stateMap != null) {
            _engine.value = GameEngine(GameStateSerializer.fromMap(stateMap))
            return
        }

        if (guestFactionName == null) {
            _waitingMessage.value = "Waiting for opponent to join... Room code: $roomCode"
            return
        }

        if (mySide == Side.PLAYER_ONE && hostFactionName != null && !gameInitialized) {
            gameInitialized = true
            val hostFaction = Faction.valueOf(hostFactionName)
            val guestFaction = Faction.valueOf(guestFactionName)
            val engine = GameEngine.newGame(
                playerOneChampion = CardSet.championFor(hostFaction),
                playerOneDeck = CardSet.buildPreconstructedDeck(hostFaction),
                playerTwoChampion = CardSet.championFor(guestFaction),
                playerTwoDeck = CardSet.buildPreconstructedDeck(guestFaction),
            )
            repository.pushState(roomCode, GameStateSerializer.toMap(engine.state))
        }
    }

    fun isInteractive(): Boolean {
        val engine = _engine.value ?: return false
        return engine.state.winner == null && engine.state.currentTurn == mySide
    }

    fun onHandCardTapped(index: Int) {
        val engine = _engine.value ?: return
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
        syncAndPush()
    }

    fun onOwnLaneTapped(lane: Int) {
        val engine = _engine.value ?: return
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
        syncAndPush()
    }

    fun onEnemyLaneTapped(lane: Int) {
        val engine = _engine.value ?: return
        if (!isInteractive()) return
        val attackerLane = _selectedAttackerLane.value ?: return
        report(engine.attack(attackerLane, lane))
        _selectedAttackerLane.value = null
        syncAndPush()
    }

    fun cancelPending() {
        _pending.value = null
        _selectedAttackerLane.value = null
    }

    fun useChampionPower() {
        val engine = _engine.value ?: return
        if (!isInteractive()) return
        val player = engine.state.current()
        report(engine.useChampionPower(autoTarget(player.champion.powerEffect, player.side)))
        syncAndPush()
    }

    fun endTurn() {
        val engine = _engine.value ?: return
        if (!isInteractive()) return
        engine.endTurn()
        _pending.value = null
        _selectedAttackerLane.value = null
        _message.value = null
        syncAndPush()
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

    /** Pushes the mutated state to Firestore and rewraps it locally so Compose recomposes immediately. */
    private fun syncAndPush() {
        val engine = _engine.value ?: return
        repository.pushState(roomCode, GameStateSerializer.toMap(engine.state))
        _engine.value = GameEngine(engine.state)
    }
}
