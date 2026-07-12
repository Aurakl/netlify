package com.riftchampions.app.online

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlin.random.Random

/**
 * Thin wrapper around a single Firestore collection ("games") used for turn-based sync.
 * Each room is one document: { hostFaction, guestFaction, state } where `state` is the
 * output of [com.riftchampions.engine.GameStateSerializer.toMap], overwritten wholesale after
 * every move. That's enough for a turn-based 1v1 game and avoids needing a custom operation log.
 *
 * Requires a Firebase project (see ../../../../../../README.md) — check
 * `com.riftchampions.app.BuildConfig.FIREBASE_CONFIGURED` before using this class.
 */
class FirebaseOnlineRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private fun roomRef(code: String) = db.collection("games").document(code)

    fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..5).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    fun createRoom(code: String, hostFaction: String, onDone: (Boolean) -> Unit) {
        val doc = mapOf(
            "hostFaction" to hostFaction,
            "guestFaction" to null,
            "state" to null,
        )
        roomRef(code).set(doc)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    fun joinRoom(code: String, guestFaction: String, onDone: (Boolean) -> Unit) {
        roomRef(code).update("guestFaction", guestFaction)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    fun pushState(code: String, stateMap: Map<String, Any?>) {
        roomRef(code).update("state", stateMap)
    }

    fun listen(code: String, onUpdate: (Map<String, Any?>?) -> Unit): ListenerRegistration {
        return roomRef(code).addSnapshotListener { snapshot, _ ->
            onUpdate(snapshot?.data)
        }
    }
}
