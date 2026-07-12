package com.riftchampions.app.online

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riftchampions.app.BuildConfig
import com.riftchampions.app.ui.theme.Ember
import com.riftchampions.engine.Faction
import com.riftchampions.engine.Side

@Composable
fun OnlineLobbyScreen(
    initialFaction: Faction,
    onGameReady: (OnlineGameViewModel) -> Unit,
    onBack: () -> Unit,
) {
    if (!BuildConfig.FIREBASE_CONFIGURED) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text("Online play isn't configured", color = Ember, fontWeight = FontWeight.Bold)
            Text(
                "This build has no google-services.json, so there's no Firebase project to sync games through. " +
                    "See the project README for how to create a free Firebase project and enable it.",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Back") }
        }
        return
    }

    val repository = remember { FirebaseOnlineRepository() }
    var roomCodeInput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var vm by remember { mutableStateOf<OnlineGameViewModel?>(null) }

    val activeVm = vm
    if (activeVm != null) {
        val waiting by activeVm.waitingMessage
        val engine by activeVm.engineState
        LaunchedEffect(engine) {
            if (engine != null) onGameReady(activeVm)
        }
        if (engine == null) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Text(waiting, color = MaterialTheme.colorScheme.onBackground)
                Button(onClick = { activeVm.dispose(); vm = null }, modifier = Modifier.padding(top = 16.dp)) { Text("Cancel") }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Play Online", color = Ember, fontWeight = FontWeight.Bold, fontSize = 22.sp)

        Button(
            onClick = {
                val code = repository.generateRoomCode()
                repository.createRoom(code, initialFaction.name) { ok ->
                    if (ok) {
                        vm = OnlineGameViewModel(code, Side.PLAYER_ONE, repository).also { it.start() }
                    } else {
                        status = "Could not create a room. Check your connection and try again."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) { Text("Create Room") }

        Text("— or —", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(vertical = 12.dp))

        OutlinedTextField(
            value = roomCodeInput,
            onValueChange = { roomCodeInput = it.uppercase() },
            label = { Text("Room code") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                val code = roomCodeInput.trim()
                if (code.isEmpty()) {
                    status = "Enter a room code first."
                    return@Button
                }
                repository.joinRoom(code, initialFaction.name) { ok ->
                    if (ok) {
                        vm = OnlineGameViewModel(code, Side.PLAYER_TWO, repository).also { it.start() }
                    } else {
                        status = "Could not join that room. Check the code and try again."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Join Room") }

        status?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }

        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) { Text("Back") }
    }
}
