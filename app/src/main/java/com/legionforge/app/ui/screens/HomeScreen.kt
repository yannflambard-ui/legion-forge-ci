package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNewList: (GameSystem) -> Unit, onOpenList: (String) -> Unit, onSettings: () -> Unit, onSearch: () -> Unit, vm: ArmyBuilderViewModel = viewModel()) {
    val lists by vm.allLists.collectAsState()
    val loading by vm.loading.collectAsState()
    val catalogError by vm.catalogError.collectAsState()
    Scaffold(topBar = {
            TopAppBar(title = { Text("LEGION FORGE", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Text("\u2699", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium)
                    }
                })
        }) { pad ->
            Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111827), Color(0xFF080B12)))).padding(pad).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Barre de recherche compacte, bien visible
                Card(onClick = onSearch, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDD0E", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(10.dp))
                        Text("Rechercher une carte, un mot-clé, une règle…", color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactGameTile("LEGION", "Armées • V2 • 1000 pts", GameSystem.LEGION_V2, Modifier.weight(1f), onClick = { onNewList(GameSystem.LEGION_V2) })
                    CompactGameTile("ARMADA", "Flottes • V1.5", GameSystem.ARMADA_V15, Modifier.weight(1f), onClick = { onNewList(GameSystem.ARMADA_V15) })
                }
            Text("LISTES RÉCENTES", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelLarge)
            if (lists.isEmpty()) Text("Vos compositions sauvegardées apparaîtront ici, hors ligne.", color = Color.LightGray)
            if (loading) {
                Spacer(Modifier.height(40.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), color = Color(0xFFFFC857))
                Spacer(Modifier.height(8.dp))
                Text("Chargement du catalogue hors ligne…", color = Color(0xFF9EACBC), style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (catalogError != null) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2224))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Erreur catalogue", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(catalogError ?: "", color = Color(0xFFFFC7B7), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            lists.forEach { list ->
                OutlinedButton(onClick = { onOpenList(list.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(list.name, color = Color.White)
                        Text("${GameSystem.valueOf(list.gameSystem).label()} • ${list.factionId} • ${list.pointsLimit} pts", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Mode hors ligne activé • données stockées sur cet appareil", color = Color(0xFF8F9BAD), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CompactGameTile(title: String, subtitle: String, system: GameSystem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(gameIconRes(system)),
                contentDescription = "Icône $title",
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

fun GameSystem.label() = if (this == GameSystem.LEGION_V2) "LEGION V2" else "ARMADA V1.5"
