package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legionforge.app.data.model.CardDefinition
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.data.model.CardKind

/** Recherche globale plein texte sur toutes les cartes (Legion + Armada) :
 *  nom, texte de règles, stats, mots-clés. Interface compacte.
 *  Cliquer un résultat ouvre une bottom sheet listant les variantes du même nom de base. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onCardClick: (CardDefinition) -> Unit,
    vm: com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel
) {
    var query by remember { mutableStateOf("") }
    val results by vm.searchResults.collectAsState()
    val searching by vm.searching.collectAsState()
    var selected by remember { mutableStateOf<CardDefinition?>(null) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("RECHERCHER", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E15))
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).padding(pad).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Barre de recherche compacte
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; vm.searchCards(it) },
                placeholder = { Text("Nom, mot-clé, règle, arme…", color = Color(0xFF5A6A7A), fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            when {
                searching -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFFC857), strokeWidth = 2.dp) }
                results.isEmpty() && query.isNotBlank() -> Text("Aucun résultat pour « $query »", color = Color(0xFF5A6A7A), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 12.dp))
                results.isNotEmpty() -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(results) { card ->
                        SearchResultRow(card, onClick = { selected = card })
                    }
                }
            }
        }
    }

    // Bottom sheet : variantes du même nom de base + fiche de la carte sélectionnée
    selected?.let { sel ->
        val variants = results.filter { baseName(it) == baseName(sel) }
        AlertDialog(
            onDismissRequest = { selected = null },
            containerColor = Color(0xFF192330),
            title = { Text(baseName(sel), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${variants.size} variante(s) — touchez pour ouvrir", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
                    variants.forEach { v ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (v.id == sel.id) Color(0xFF2A3A4A) else Color(0xFF0A0E15)).clickable { onCardClick(v); selected = null }.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(v.displayName(), color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("${kindLabel(v)} • ${gameLabel(v)}", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
                            }
                            Text("${v.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Fermer", color = Color(0xFFFFC857)) } }
        )
    }
}

@Composable
private fun SearchResultRow(card: CardDefinition, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(card.displayName(), color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(kindLabel(card), color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(gameLabel(card), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("${card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

/** Nom de base : retire le suffixe de variante entre parenthèses (ex "Wookiee Warriors (Freedom Fighters)" → "Wookiee Warriors"). */
private fun baseName(card: CardDefinition): String = card.name.substringBefore(" (").trim()

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
