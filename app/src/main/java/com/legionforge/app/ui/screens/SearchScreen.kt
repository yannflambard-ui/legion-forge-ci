package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.legionforge.app.data.model.CardDefinition
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.data.model.CardKind

/** Recherche globale plein texte sur toutes les cartes (Legion + Armada) :
 *  nom, texte de règles, stats, mots-clés. */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onCardClick: (CardDefinition) -> Unit,
    vm: com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel
) {
    var query by remember { mutableStateOf("") }
    val results by vm.searchResults.collectAsState()
    val searching by vm.searching.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("RECHERCHER UNE CARTE", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E15))
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).padding(pad).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; vm.searchCards(it) },
                placeholder = { Text("Nom, mot-clé, règle, arme… (ex: Pierce, Dodge, Vader)", color = Color(0xFF5A6A7A)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            Text("Trouve une carte par nom, effet, mot-clé ou statistique — sur les deux jeux.", color = Color(0xFF8F9BAD), style = MaterialTheme.typography.labelSmall)
            when {
                searching -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFFC857)) }
                results.isEmpty() && query.isNotBlank() -> Text("Aucun résultat pour « $query »", color = Color(0xFF5A6A7A), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp))
                results.isNotEmpty() -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(results) { card ->
                        SearchResultRow(card, onClick = { onCardClick(card) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(card: CardDefinition, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(card.displayName(), color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(kindLabel(card), color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(gameLabel(card), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("${card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private fun kindLabel(card: CardDefinition): String = when (card.kind) {
    CardKind.LEGION_UNIT -> "Unité"
    CardKind.LEGION_UPGRADE -> "Amélioration"
    CardKind.ARMADA_SHIP -> "Vaisseau"
    CardKind.ARMADA_SQUADRON -> "Escadron"
    CardKind.ARMADA_UPGRADE -> "Amélioration"
    CardKind.COMMANDER -> "Commandant"
    else -> "Carte"
}
private fun gameLabel(card: CardDefinition): String = if (card.gameSystem == GameSystem.LEGION_V2) "LEGION V2" else "ARMADA V1.5"
