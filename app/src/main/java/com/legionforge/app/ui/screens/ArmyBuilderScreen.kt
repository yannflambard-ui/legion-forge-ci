package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.legionforge.app.data.model.*
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmyBuilderScreen(listId: String, onBack: () -> Unit, viewModel: ArmyBuilderViewModel = viewModel()) {
    val list by viewModel.currentList.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val validation by viewModel.validation.collectAsState()
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    LaunchedEffect(listId) { viewModel.openList(listId) }
    val game = list?.gameSystem?.let { runCatching { GameSystem.valueOf(it) }.getOrNull() } ?: GameSystem.LEGION_V2
    val allowedKinds = if (game == GameSystem.LEGION_V2) setOf(CardKind.LEGION_UNIT, CardKind.LEGION_UPGRADE) else setOf(CardKind.ARMADA_SHIP, CardKind.ARMADA_SQUADRON, CardKind.ARMADA_UPGRADE, CardKind.COMMANDER)
    val additions = cards.filter { it.kind in allowedKinds }
        .filter { it.name.contains(search, ignoreCase = true) || it.factionId.contains(search, ignoreCase = true) }
    Scaffold(topBar = {
        TopAppBar(title = { Column {
            Text(list?.name ?: "Nouvelle liste", style = MaterialTheme.typography.titleLarge)
            Text("${game.label()}  •  ${list?.factionId.orEmpty()}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC857))
        } }, navigationIcon = { TextButton(onClick = onBack) { Text("‹") } })
    }) { pad ->
        Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).padding(pad)) {
            val total = validation.totalPoints
            val limit = list?.pointsLimit ?: if (game == GameSystem.LEGION_V2) 1000 else 400
            Card(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("$total", color = Color(0xFFFFC857), style = MaterialTheme.typography.headlineMedium)
                            Text("POINTS / $limit", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        }
                        val count = entries.filter { it.card.kind == if (game == GameSystem.LEGION_V2) CardKind.LEGION_UNIT else CardKind.ARMADA_SHIP }.sumOf { it.quantity }
                        Text("$count ${if (game == GameSystem.LEGION_V2) "unités" else "vaisseaux"}", color = Color.White)
                    }
                    if (game == GameSystem.ARMADA_V15) {
                        val squadronPts = entries.filter { it.card.kind == CardKind.ARMADA_SQUADRON }.sumOf { it.card.points * it.quantity }
                        LinearProgressIndicator(progress = { (squadronPts.toFloat() / ((limit + 2) / 3).coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Color(0xFFFFC857))
                        Text("Escadrons $squadronPts / ${(limit + 2) / 3} pts", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    } else {
                        val counts = entries.filter { it.card.kind == CardKind.LEGION_UNIT }.groupBy { it.card.legionRank }.mapValues { (_, items) -> items.sumOf { it.quantity } }
                        Text("C ${counts[LegionRank.COMMANDER] ?: 0}/1–2   •   T ${counts[LegionRank.CORPS] ?: 0}/3–6   •   FS ${counts[LegionRank.SPECIAL_FORCES] ?: 0}/0–3", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (validation.violations.isNotEmpty()) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2224))) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        validation.violations.take(3).forEach { Text("! ${it.message}", color = Color(0xFFFFC7B7), style = MaterialTheme.typography.bodySmall) }
                        if (validation.violations.size > 3) Text("+ ${validation.violations.size - 3} règles à corriger", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else if (entries.isNotEmpty()) Text("✓ Liste valide", Modifier.padding(horizontal = 18.dp, vertical = 5.dp), color = Color(0xFF77D9A7))
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("LISTE (${entries.size})") })
                Tab(selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("CATALOGUE (${additions.size})") })
            }
            if (selectedTab == 0) {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (entries.isEmpty()) item { Text("Votre force est vide. Ouvrez le catalogue pour ajouter vos premières cartes.", color = Color.LightGray, modifier = Modifier.padding(16.dp)) }
                    items(entries, key = { it.instanceId }) { entry -> BuilderEntryCard(entry, onRemove = { viewModel.remove(entry) }) }
                }
            } else {
                OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), placeholder = { Text("Rechercher une carte…") }, singleLine = true)
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(additions, key = { it.id }) { card -> CatalogCard(card, entries, onAdd = { parent, slot -> viewModel.add(card, parent, slot) }) }
                }
            }
            Text("Hors ligne • catalogue sous réserve des mises à jour officielles", Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp), color = Color(0xFF718096), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun BuilderEntryCard(entry: ListEntry, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF18212D))) {
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CardArtwork(entry.card, Modifier.size(width = 64.dp, height = 88.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.card.name, color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text("${entry.card.points * entry.quantity} pts  •  ${entry.card.legionRank?.name?.replace('_', ' ') ?: entry.card.kind.name.replace('_', ' ')}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall)
                if (entry.parentInstanceId != null) Text("↳ ${entry.chosenSlot?.name?.replace('_', ' ') ?: "amélioration liée"}", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                if (entry.card.kind == CardKind.LEGION_UNIT || entry.card.kind == CardKind.ARMADA_SHIP) {
                    if (entry.card.allowedUpgradeSlots.isNotEmpty()) Text("Slots : ${entry.card.allowedUpgradeSlots.joinToString { it.name.lowercase().replace('_', ' ') }}", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, maxLines = 2)
                }
            }
            TextButton(onClick = onRemove) { Text("RETIRER", color = Color(0xFFFF927F), style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun CatalogCard(card: CardDefinition, entries: List<ListEntry>, onAdd: (String?, ArmadaSlot?) -> Unit) {
    val armadaShip = card.kind == CardKind.ARMADA_SHIP
    val isUpgrade = card.kind == CardKind.LEGION_UPGRADE || card.kind == CardKind.ARMADA_UPGRADE
    val isCommander = card.kind == CardKind.COMMANDER
    val requiresTarget = isUpgrade && !isCommander
    val targetUnits = if (card.kind == CardKind.LEGION_UPGRADE) entries.filter { it.card.kind == CardKind.LEGION_UNIT } else entries.filter { it.card.kind == CardKind.ARMADA_SHIP }
    val eligibleTargets = if (card.kind == CardKind.LEGION_UPGRADE) targetUnits.filter { target -> card.upgradeSlots.firstOrNull()?.let { it in target.card.allowedUpgradeSlots } == true } else if (card.kind == CardKind.ARMADA_UPGRADE) targetUnits.filter { target -> card.upgradeSlots.any { it in target.card.allowedUpgradeSlots } } else targetUnits
    var targetId by remember(card.id, eligibleTargets.size) { mutableStateOf(eligibleTargets.firstOrNull()?.instanceId) }
    var selectedSlot by remember(card.id) { mutableStateOf(card.upgradeSlots.firstOrNull()) }
    val eligibleSlots = if (card.kind == CardKind.ARMADA_UPGRADE) card.upgradeSlots.filter { slot -> eligibleTargets.any { target -> slot in target.card.allowedUpgradeSlots } } else if (card.kind == CardKind.LEGION_UPGRADE) card.upgradeSlots.filter { slot -> eligibleTargets.any { target -> slot in target.card.allowedUpgradeSlots } } else card.upgradeSlots
    val slotCounts = entries.filter { it.parentInstanceId != null && (it.card.kind == CardKind.ARMADA_UPGRADE || it.card.kind == CardKind.LEGION_UPGRADE) }.groupBy { it.parentInstanceId to (it.chosenSlot ?: it.card.upgradeSlots.firstOrNull()) }.mapValues { (_, items) -> items.sumOf { it.quantity } }
    val addingTo = eligibleTargets.firstOrNull { it.instanceId == targetId }
    val validSelection = (!requiresTarget || addingTo != null) && (!isUpgrade || (selectedSlot != null && addingTo != null && selectedSlot in addingTo.card.allowedUpgradeSlots && (slotCounts[targetId to selectedSlot] ?: 0) < addingTo.card.allowedUpgradeSlots.count { it == selectedSlot }))
    val alreadyAdded = entries.any { it.card.id == card.id && (card.unique || card.kind == CardKind.COMMANDER) }
    val compatibleSlotFull = eligibleSlots.isNotEmpty() && eligibleSlots.all { slot -> eligibleTargets.all { target -> (slotCounts[target.instanceId to slot] ?: 0) >= target.card.allowedUpgradeSlots.count { it == slot } } }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF18212D))) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
            CardArtwork(card, Modifier.size(width = 58.dp, height = 80.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(card.name, color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text("${card.points} pts • ${card.kind.name.replace('_', ' ')}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall)
                Text(card.rulesText.orEmpty().ifBlank { "${card.factionId.replace('-', ' ')} • ${if (card.unique) "Unique" else "Standard"}" }, maxLines = 2, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                if (requiresTarget && eligibleTargets.isNotEmpty()) {
                    var expandedTarget by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expandedTarget = true }) { Text("Pour : ${eligibleTargets.firstOrNull { it.instanceId == targetId }?.card?.name ?: "choisir unité"}") }
                        DropdownMenu(expandedTarget, onDismissRequest = { expandedTarget = false }) {
                            eligibleTargets.forEach { target -> DropdownMenuItem(text = { Text(target.card.name) }, onClick = { targetId = target.instanceId; expandedTarget = false }) }
                        }
                    }
                    if (armadaShip && card.allowedUpgradeSlots.isNotEmpty()) Text("Slots : ${card.allowedUpgradeSlots.groupingBy { it }.eachCount().entries.joinToString { "${it.value}× ${it.key.name.lowercase().replace('_', ' ')}" }}", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
                    if (card.kind == CardKind.ARMADA_UPGRADE && eligibleSlots.size > 1) {
                        var expandedSlot by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { expandedSlot = true }) { Text("Slot : ${selectedSlot?.name?.replace('_', ' ') ?: "sélectionner"}") }
                            DropdownMenu(expandedSlot, onDismissRequest = { expandedSlot = false }) {
                                eligibleSlots.forEach { slot -> DropdownMenuItem(text = { Text(slot.name.replace('_', ' ')) }, onClick = { selectedSlot = slot; expandedSlot = false }) }
                            }
                        }
                    }
                    if (requiresTarget && eligibleTargets.isEmpty()) Text(if (card.kind == CardKind.LEGION_UPGRADE) "Ajoutez d'abord une unité avec ce slot" else "Aucun vaisseau compatible dans la flotte", color = Color(0xFFFF927F), style = MaterialTheme.typography.labelSmall)
                    if (compatibleSlotFull) Text("Tous les slots compatibles sont occupés", color = Color(0xFFFF927F), style = MaterialTheme.typography.labelSmall)
                }
            }
            Button(enabled = (!requiresTarget || eligibleTargets.isNotEmpty()) && !alreadyAdded && !compatibleSlotFull && validSelection, onClick = { onAdd(if (requiresTarget) targetId else null, selectedSlot) }) { Text("+") }
        }
    }
}

@Composable
private fun CardArtwork(card: CardDefinition, modifier: Modifier = Modifier) {
    val source: Any? = card.imageAssetPath?.let { "file:///android_asset/$it" } ?: card.imageUrl
    if (source != null) AsyncImage(model = source, contentDescription = "Visuel de ${card.name}", modifier = modifier, contentScale = ContentScale.Crop)
    else Card(modifier, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF253344))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF253344), Color(0xFF111820)))), contentAlignment = Alignment.Center) {
            Text(card.name.split(' ').take(2).joinToString("\n"), color = Color(0xFF8494A8), style = MaterialTheme.typography.labelSmall)
        }
    }
}
