package com.riftchampions.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riftchampions.app.ui.theme.Ember
import com.riftchampions.app.ui.theme.Surface
import com.riftchampions.engine.Faction

private fun factionLabel(faction: Faction): String = when (faction) {
    Faction.ASHEN_VANGUARD -> "Ashen Vanguard"
    Faction.VERDANT_WARDENS -> "Verdant Wardens"
    Faction.HOLLOW_CHOIR -> "Hollow Choir"
    Faction.NEUTRAL -> "Neutral"
}

private val PLAYABLE_FACTIONS = listOf(Faction.ASHEN_VANGUARD, Faction.VERDANT_WARDENS, Faction.HOLLOW_CHOIR)

@Composable
fun MenuScreen(
    onStartVsAi: (Faction, Faction) -> Unit,
    onStartHotseat: (Faction, Faction) -> Unit,
    onGoOnline: (Faction) -> Unit,
) {
    var yourFaction by remember { mutableStateOf(Faction.ASHEN_VANGUARD) }
    var opponentFaction by remember { mutableStateOf(Faction.VERDANT_WARDENS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
    ) {
        Text("Rift Champions", color = Ember, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(
            "A lane-based card duel. Original cards, no relation to any existing card game.",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text("Your faction", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        FactionPicker(selected = yourFaction, onSelect = { yourFaction = it })

        Spacer(modifier = Modifier.height(16.dp))
        Text("Opponent faction (Pass & Play only)", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        FactionPicker(selected = opponentFaction, onSelect = { opponentFaction = it })

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val remaining = PLAYABLE_FACTIONS.filter { it != yourFaction }
                onStartVsAi(yourFaction, remaining.random())
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Practice vs AI") }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { onStartHotseat(yourFaction, opponentFaction) },
            modifier = Modifier.fillMaxWidth(),
            enabled = yourFaction != opponentFaction,
        ) { Text("Pass & Play (same device)") }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { onGoOnline(yourFaction) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Play Online") }
    }
}

@Composable
private fun FactionPicker(selected: Faction, onSelect: (Faction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        for (faction in PLAYABLE_FACTIONS) {
            val isSelected = faction == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(Surface, RoundedCornerShape(8.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Ember else Surface,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(faction) }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(factionLabel(faction), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}
