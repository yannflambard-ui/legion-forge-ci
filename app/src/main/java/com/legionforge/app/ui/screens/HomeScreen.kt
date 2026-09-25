package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNewList: (GameSystem) -> Unit, onOpenList: (String) -> Unit, onSettings: () -> Unit, vm: ArmyBuilderViewModel = viewModel()) {
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
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111827), Color(0xFF080B12)))).padding(pad).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("PRÉPAREZ LA BATAILLE", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelLarge)
            Text("Vos listes.\nVotre stratégie.", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            GameTile("LEGION", "Armées • règles V2 • 1 000 points", "01", onClick = { onNewList(GameSystem.LEGION_V2) })
            GameTile("ARMADA", "Flottes • règles V1.5 • commandez la galaxie", "02", onClick = { onNewList(GameSystem.ARMADA_V15) })
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
private fun GameTile(title: String, subtitle: String, number: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(title, color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = Color(0xFFCFD6E2), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onClick) { Text("NOUVELLE LISTE  →") }
            }
            Text(number, color = Color(0xFF46566A), style = MaterialTheme.typography.displaySmall)
        }
    }
}

fun GameSystem.label() = if (this == GameSystem.LEGION_V2) "LEGION V2" else "ARMADA V1.5"
