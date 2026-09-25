package com.legionforge.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legionforge.app.R
import com.legionforge.app.data.model.CardDefinition
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FactionPickerScreen(system: GameSystem, onFactionSelected: (String) -> Unit, vm: ArmyBuilderViewModel = viewModel()) {
    val cards by vm.cards.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.catalogError.collectAsState()
    androidx.compose.runtime.LaunchedEffect(system) { vm.loadAllCatalog(system) }
    val factionCards = cards.filter { if (system == GameSystem.LEGION_V2) it.kind == com.legionforge.app.data.model.CardKind.LEGION_UNIT else it.kind == com.legionforge.app.data.model.CardKind.ARMADA_SHIP }
    val groups = factionCards.groupBy { it.factionId }
    Scaffold(topBar = { TopAppBar(title = { Text("Choisir une faction") }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(system.label(), color = Color(0xFFFFC857), style = MaterialTheme.typography.labelLarge) }
            items(groups.keys.sorted(), key = { it }) { faction ->
                val count = groups[faction]?.count { it.kind == if (system == GameSystem.LEGION_V2) com.legionforge.app.data.model.CardKind.LEGION_UNIT else com.legionforge.app.data.model.CardKind.ARMADA_SHIP } ?: 0
                Card(onClick = { onFactionSelected(faction) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF192331))) {
                    Row(Modifier.padding(18.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(factionIconRes(faction)),
                            contentDescription = "Icône ${faction.displayName()}",
                            modifier = Modifier.size(64.dp)
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(faction.displayName(), style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Text("$count ${if (system == GameSystem.LEGION_V2) "unités / véhicules" else "vaisseaux"} au catalogue  →", color = Color(0xFFFFC857))
                        }
                    }
                }
            }
            if (cards.isEmpty()) {
    if (loading) {
        item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), color = Color(0xFFFFC857)) }
        item { Text("Chargement du catalogue hors ligne…", color = Color.LightGray) }
    }
    if (error != null) {
        item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2224))) {
            Column(Modifier.padding(12.dp)) {
                Text("Erreur catalogue", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium)
                Text(error ?: "", color = Color(0xFFFFC7B7), style = MaterialTheme.typography.bodySmall)
            }
        } }
    }
    if (!loading && error == null) {
        item { Text("Catalogue vide. Reessayez plus tard.", color = Color.LightGray) }
    }
}
        }
    }
}

private fun String.displayName() = split('-', '_').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

/** Ressource drawable de l'icône d'une faction (abstraite, pas de logo officiel - DMCA safe). */
internal fun factionIconRes(faction: String): Int = when (faction) {
    "rebel" -> R.drawable.ic_faction_rebel
    "republic", "republics" -> R.drawable.ic_faction_republic
    "separatist", "separatists" -> R.drawable.ic_faction_separatist
    "neutral" -> R.drawable.ic_faction_neutral
    else -> R.drawable.ic_faction_empire
}

/** Ressource drawable de l'icône d'un jeu (LEGION / ARMADA). */
internal fun gameIconRes(system: GameSystem): Int =
    if (system == GameSystem.LEGION_V2) R.drawable.ic_game_legion else R.drawable.ic_game_armada
