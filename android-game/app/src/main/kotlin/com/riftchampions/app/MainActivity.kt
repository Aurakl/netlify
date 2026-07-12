package com.riftchampions.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.riftchampions.app.online.OnlineGameScreen
import com.riftchampions.app.online.OnlineGameViewModel
import com.riftchampions.app.online.OnlineLobbyScreen
import com.riftchampions.app.ui.GameScreen
import com.riftchampions.app.ui.MenuScreen
import com.riftchampions.app.ui.theme.RiftChampionsTheme
import com.riftchampions.engine.Faction

private sealed class Screen {
    data object Menu : Screen()
    data class Local(val viewModel: GameViewModel) : Screen()
    data class OnlineLobby(val faction: Faction) : Screen()
    data class Online(val viewModel: OnlineGameViewModel) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RiftChampionsTheme {
                var screen: Screen by remember { mutableStateOf(Screen.Menu) }

                when (val current = screen) {
                    is Screen.Menu -> MenuScreen(
                        onStartVsAi = { yours, opponent ->
                            screen = Screen.Local(GameViewModel(yours, opponent, GameMode.VS_AI))
                        },
                        onStartHotseat = { p1, p2 ->
                            screen = Screen.Local(GameViewModel(p1, p2, GameMode.LOCAL_HOTSEAT))
                        },
                        onGoOnline = { faction -> screen = Screen.OnlineLobby(faction) },
                    )
                    is Screen.Local -> GameScreen(
                        viewModel = current.viewModel,
                        onExit = { screen = Screen.Menu },
                    )
                    is Screen.OnlineLobby -> OnlineLobbyScreen(
                        initialFaction = current.faction,
                        onGameReady = { onlineVm -> screen = Screen.Online(onlineVm) },
                        onBack = { screen = Screen.Menu },
                    )
                    is Screen.Online -> OnlineGameScreen(
                        viewModel = current.viewModel,
                        onExit = { screen = Screen.Menu },
                    )
                }
            }
        }
    }
}
