package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legionforge.app.R
import com.legionforge.app.data.model.CardDefinition
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.data.repository.CollectionRepository
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel

/**
 * Ma collection (Réglages) : cocher le matériel acheté, carte par carte,
 * groupé par système puis faction, avec recherche. Feed propagé au catalogue
 * via CollectionRepository.owned (le builder peut filtrer "ma collection").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(onBack: () -> Unit, vm: ArmyBuilderViewModel = viewModel()) {
    val allCards by vm.allCatalog.collectAsState()
    val owned by CollectionRepository.owned.collectAsState()
    var search by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.loadAllCatalogForCollection() }

    val q = search.trim()
    val filtered = allCards.filter {
        q.isEmpty() || it.displayName().contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true) || it.factionId.contains(q, ignoreCase = true)
    }
    // Groupement stable : système, puis faction, puis type (unités avant upgrades).
    val grouped = filtered.groupBy { it.gameSystem }
        .toSortedMap(compareBy { it.ordinal })
        .map { (system, cards) ->
            system to cards.sortedWith(compareBy({ it.factionId }, { it.kind.ordinal }, { it.name.lowercase() }))
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_collection)) },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹") } }
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).padding(pad)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "$owned (${owned.size})",
                    color = Color(0xFFFFC857),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                grouped.forEach { (system, cardsInSystem) ->
                    item(key = "sys_${system.name}") {
                        Text(
                            system.label(),
                            color = Color(0xFFFFC857),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                    var lastFaction: String? = null
                    cardsInSystem.forEach { card ->
                        if (card.factionId != lastFaction) {
                            lastFaction = card.factionId
                            item(key = "fac_${system.name}_${card.factionId}") {
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        factionLabel(card.factionId),
                                        color = FactionCardColor(card.factionId),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(FactionCardColor(card.factionId).copy(alpha = 0.18f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        item(key = card.id) {
                            CollectionRow(card, owned.contains(card.id)) { CollectionRepository.toggle(card.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionRow(card: CardDefinition, isOwned: Boolean, onToggle: () -> Unit) {
    val accent = FactionCardColor(card.factionId)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle),
        color = if (isOwned) accent.copy(alpha = 0.12f) else Color(0xFF141B26)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isOwned,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = accent, checkmarkColor = Color.Black)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    card.displayName(),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(kindLabel(card.kind), color = Color(0xFF8F9BAD), style = MaterialTheme.typography.labelSmall)
                    if (card.kind.name == "LEGION_UNIT" && card.legionRank != null) {
                        Text(rankLabel(card.legionRank), color = rankColor(card.legionRank), style = MaterialTheme.typography.labelSmall)
                    }
                    Text("${card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}