package com.riftchampions.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riftchampions.app.GameMode
import com.riftchampions.app.GameViewModel
import com.riftchampions.app.ui.theme.Danger
import com.riftchampions.app.ui.theme.Ember
import com.riftchampions.app.ui.theme.Hollow
import com.riftchampions.app.ui.theme.Surface
import com.riftchampions.app.ui.theme.Verdant
import com.riftchampions.engine.CardDef
import com.riftchampions.engine.CardType
import com.riftchampions.engine.CreatureInstance
import com.riftchampions.engine.Faction
import com.riftchampions.engine.GameState
import com.riftchampions.engine.Keyword
import com.riftchampions.engine.LANE_COUNT
import com.riftchampions.engine.PlayerState
import com.riftchampions.engine.Side

private fun factionColor(faction: Faction): Color = when (faction) {
    Faction.ASHEN_VANGUARD -> Ember
    Faction.VERDANT_WARDENS -> Verdant
    Faction.HOLLOW_CHOIR -> Hollow
    Faction.NEUTRAL -> Color(0xFFB8B0C4)
}

private fun keywordTag(keyword: Keyword): String = when (keyword) {
    Keyword.CHARGE -> "CHG"
    Keyword.RANGED -> "RNG"
    Keyword.LIFESTEAL -> "LST"
    Keyword.POISON -> "PSN"
}

@Composable
fun GameScreen(viewModel: GameViewModel, onExit: () -> Unit) {
    // Reading .value here subscribes this whole composable to state changes; GameBoard then reads
    // viewModel.engine.state fresh below (that plain state isn't itself Compose-observable).
    @Suppress("UNUSED_VARIABLE") val rev = viewModel.revision.value
    val pending by viewModel.pending
    val selectedAttacker by viewModel.selectedAttackerLane
    val message by viewModel.message

    val isHotseat = viewModel.mode == GameMode.LOCAL_HOTSEAT
    GameBoard(
        state = viewModel.engine.state,
        youSide = if (isHotseat) viewModel.engine.state.currentTurn else Side.PLAYER_ONE,
        perspective = if (isHotseat) null else Side.PLAYER_ONE,
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

/**
 * Pure rendering of one match. [youSide] picks which side is shown at the bottom of the screen:
 * pass-and-play passes [GameState.currentTurn] so the active hand/board flips to the bottom
 * automatically as the device is handed over, while vs-AI and online play pass a fixed side
 * since only one physical player is ever looking at this screen.
 */
@Composable
fun GameBoard(
    state: GameState,
    youSide: Side,
    /** Whose result framing to show on game over: a fixed side ("Victory"/"Defeat"), or null for
     *  a neutral "Player X wins" message when both sides are human on the same device. */
    perspective: Side?,
    pendingHandIndex: Int?,
    selectedAttackerLane: Int?,
    message: String?,
    interactive: Boolean,
    onHandCardTapped: (Int) -> Unit,
    onOwnLaneTapped: (Int) -> Unit,
    onEnemyLaneTapped: (Int) -> Unit,
    onChampionPower: () -> Unit,
    onEndTurn: () -> Unit,
    onCancel: () -> Unit,
    onExit: () -> Unit,
) {
    val you = state.player(youSide)
    val enemy = state.opponentOf(youSide)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
    ) {
        val winner = state.winner
        if (winner != null) {
            GameOverBanner(
                winnerName = state.player(winner).champion.name,
                youWon = perspective?.let { it == winner },
                onExit = onExit,
            )
            return@Column
        }

        ChampionBar(player = enemy, isYou = false, isCurrentTurn = enemy.side == state.currentTurn)
        LaneRow(
            player = enemy,
            selectable = interactive && selectedAttackerLane != null,
            onLaneTapped = onEnemyLaneTapped,
        )

        Box(modifier = Modifier.height(8.dp))

        LaneRow(
            player = you,
            selectable = interactive,
            highlightedLane = selectedAttackerLane,
            onLaneTapped = onOwnLaneTapped,
        )
        ChampionBar(player = you, isYou = true, isCurrentTurn = you.side == state.currentTurn)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Turn ${state.turnNumber} — ${state.current().champion.name}'s move",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp,
            )
            Button(
                onClick = onChampionPower,
                enabled = interactive && !you.championPowerUsedThisTurn && you.availableMana() >= you.champion.powerCost,
            ) {
                Text("Power (${you.champion.powerCost})")
            }
            if (interactive && (pendingHandIndex != null || selectedAttackerLane != null)) {
                Button(onClick = onCancel) { Text("Cancel") }
            }
            Button(onClick = onEndTurn, enabled = interactive) {
                Text("End Turn")
            }
        }

        message?.let {
            Text(text = it, color = Danger, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        }

        Text(
            text = "Mana: ${you.availableMana()} / ${you.resourcesInPlay}",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        HandRow(
            hand = you.hand,
            selectedIndex = pendingHandIndex,
            enabled = interactive,
            availableMana = you.availableMana(),
            onCardTapped = onHandCardTapped,
        )
    }
}

@Composable
private fun GameOverBanner(winnerName: String, youWon: Boolean?, onExit: () -> Unit) {
    val text = when (youWon) {
        true -> "Victory!"
        false -> "Defeat"
        null -> "$winnerName wins!"
    }
    val color = when (youWon) {
        true -> Verdant
        false -> Danger
        null -> Ember
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = text, color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.height(24.dp))
        Button(onClick = onExit) { Text("Back to menu") }
    }
}

@Composable
private fun ChampionBar(player: PlayerState, isYou: Boolean, isCurrentTurn: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(player.champion.name, color = factionColor(player.champion.faction), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            val label = (if (isYou) "You" else "Opponent") + if (isCurrentTurn) " — active" else ""
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
        }
        Text(
            text = "${player.championHealth} / ${player.champion.maxHealth} HP",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LaneRow(
    player: PlayerState,
    selectable: Boolean,
    highlightedLane: Int? = null,
    onLaneTapped: (Int) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (lane in 0 until LANE_COUNT) {
            LaneSlot(
                creature = player.lanes[lane],
                highlighted = highlightedLane == lane,
                selectable = selectable,
                modifier = Modifier.weight(1f),
                onClick = { onLaneTapped(lane) },
            )
        }
    }
}

@Composable
private fun LaneSlot(
    creature: CreatureInstance?,
    highlighted: Boolean,
    selectable: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.8f)
            .background(Surface, RoundedCornerShape(8.dp))
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) Ember else Color(0x33FFFFFF),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(enabled = selectable) { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (creature == null) {
            Text("Empty", color = Color(0x66FFFFFF), fontSize = 11.sp)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = creature.def.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )
                Box(modifier = Modifier.height(4.dp))
                Text("${creature.currentAttack} / ${creature.currentHealth}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                if (creature.summoningSick) {
                    Text("Sick", color = Color(0x99FFFFFF), fontSize = 9.sp)
                }
                if (creature.keywords.isNotEmpty()) {
                    Text(creature.keywords.joinToString(" ") { keywordTag(it) }, color = Ember, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun HandRow(
    hand: List<CardDef>,
    selectedIndex: Int?,
    enabled: Boolean,
    availableMana: Int,
    onCardTapped: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        hand.forEachIndexed { index, card ->
            val affordable = card.type == CardType.RESOURCE || card.cost <= availableMana
            HandCard(
                card = card,
                selected = selectedIndex == index,
                enabled = enabled && affordable,
                onClick = { onCardTapped(index) },
            )
        }
    }
}

@Composable
private fun HandCard(card: CardDef, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .height(128.dp)
            .background(Surface, RoundedCornerShape(8.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Ember else factionColor(card.faction).copy(alpha = if (enabled) 0.8f else 0.25f),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(6.dp),
    ) {
        Text(text = card.name, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 3)
        Box(modifier = Modifier.size(1.dp))
        Text(text = "Cost ${card.cost}", color = Ember.copy(alpha = if (enabled) 1f else 0.4f), fontSize = 10.sp)
        if (card.type == CardType.CREATURE || card.type == CardType.EQUIPMENT) {
            Text(text = "${card.attack}/${card.health}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f), fontSize = 10.sp)
        }
        if (card.description.isNotEmpty()) {
            Text(text = card.description, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.8f else 0.3f), fontSize = 9.sp, maxLines = 3)
        }
    }
}
