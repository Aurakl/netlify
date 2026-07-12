package com.riftchampions.app.online

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riftchampions.app.ui.GameBoard

@Composable
fun OnlineGameScreen(viewModel: OnlineGameViewModel, onExit: () -> Unit) {
    DisposableEffect(viewModel) {
        onDispose { viewModel.dispose() }
    }

    val engine by viewModel.engineState
    val pending by viewModel.pending
    val selectedAttacker by viewModel.selectedAttackerLane
    val message by viewModel.message
    val waitingMessage by viewModel.waitingMessage

    val currentEngine = engine
    if (currentEngine == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = waitingMessage, color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    GameBoard(
        state = currentEngine.state,
        youSide = viewModel.mySide,
        perspective = viewModel.mySide,
        pendingHandIndex = pending?.handIndex,
        selectedAttackerLane = selectedAttacker,
        message = message,
        interactive = viewModel.isInteractive(),
        onHandCardTapped = viewModel::onHandCardTapped,
        onOwnLaneTapped = viewModel::onOwnLaneTapped,
        onEnemyLaneTapped = viewModel::onEnemyLaneTapped,
        onChampionPower = viewModel::useChampionPower,
        onEndTurn = viewModel::endTurn,
        onCancel = viewModel::cancelPending,
        onExit = onExit,
    )
}
