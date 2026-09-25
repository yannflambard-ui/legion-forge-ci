package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.legionforge.app.data.model.*
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmyBuilderScreen(listId: String, onBack: () -> Unit, onPlayCard: (String, String) -> Unit = { _, _ -> }, viewModel: ArmyBuilderViewModel = viewModel()) {
    val list by viewModel.currentList.collectAsState()
    val cards by viewModel.cards.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val validation by viewModel.validation.collectAsState()
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedParentId by remember { mutableStateOf<String?>(null) }
                // Filtres catalogue (P0) : rang/type, tranche de points, mot-clé
    var filterRank by remember { mutableStateOf<String?>(null) }
    var filterMaxPts by remember { mutableStateOf<Int?>(null) }
    var filterKeyword by remember { mutableStateOf<String?>(null) }
    var filtersMenuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(listId) { viewModel.openList(listId) }
    val game = list?.gameSystem?.let { runCatching { GameSystem.valueOf(it) }.getOrNull() } ?: GameSystem.LEGION_V2
    val allowedKinds = if (game == GameSystem.LEGION_V2) setOf(CardKind.LEGION_UNIT, CardKind.LEGION_UPGRADE) else setOf(CardKind.ARMADA_SHIP, CardKind.ARMADA_SQUADRON, CardKind.ARMADA_UPGRADE, CardKind.COMMANDER)
    // Faction de la liste : le catalogue n'affiche que les cartes neutres ou de cette faction.
    // (Certaines cartes Armada existent en 2 factions — ex: Ahsoka Tano rebel/republic — mais
    // ce sont des cartes distinctes par faction, donc le filtre factionId les gère correctement.)
    val listFaction = list?.factionId
    fun matchesFaction(c: CardDefinition) = listFaction == null || c.factionId == listFaction || c.factionId == "neutral"
    // When a parent unit is selected, show ONLY the upgrades that fit in that parent's
    // slots (and, for Armada, that match the ship family via linkedUnit). No other ships,
    // squadrons or commanders — the catalogue is scoped to the selected parent.
    val selectedParent = entries.firstOrNull { it.instanceId == selectedParentId }
    val filteredAdditions = if (selectedParent != null) {
        cards.filter { it.kind in allowedKinds && matchesFaction(it) }.filter { c ->
            when {
                game == GameSystem.LEGION_V2 && c.kind == CardKind.LEGION_UPGRADE ->
                    c.upgradeSlots.any { it in selectedParent.card.allowedUpgradeSlots }
                game == GameSystem.ARMADA_V15 && c.kind == CardKind.ARMADA_UPGRADE ->
                    c.upgradeSlots.any { it in selectedParent.card.allowedUpgradeSlots } && upgradeFitsShip(c, selectedParent.card)
                else -> false
            }
        }
    } else cards.filter { it.kind in allowedKinds && matchesFaction(it) }
    val additions = filteredAdditions
                .filter { it.name.contains(search, ignoreCase = true) || it.factionId.contains(search, ignoreCase = true) }
                .let { list ->
                    val rk = filterRank; val mp = filterMaxPts; val kw = filterKeyword
                    list.filter { rk == null || it.legionRank?.name == rk }
                        .filter { mp == null || it.points <= mp }
                        .filter { kw == null || it.legionStats?.contains(kw, ignoreCase = true) == true }
                }
    // Bouton play global : actif (vert) uniquement si la liste est valide.
    val listValid = validation.violations.isEmpty() && entries.isNotEmpty()
    val firstPlayable = entries.firstOrNull { it.parentInstanceId == null && (it.card.kind == CardKind.LEGION_UNIT || it.card.kind == CardKind.ARMADA_SHIP) }
    Scaffold(topBar = {
        TopAppBar(title = { Column {
            Text(list?.name ?: "Nouvelle liste", style = MaterialTheme.typography.titleLarge)
            Text("${game.label()}  •  ${list?.factionId.orEmpty()}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC857))
        } }, navigationIcon = { TextButton(onClick = onBack) { Text("‹") } },
            actions = {
                // Bouton play compact dans la barre du haut, à droite du titre
                Button(
                    onClick = { if (firstPlayable != null) onPlayCard(listId, firstPlayable.instanceId) },
                    enabled = listValid && firstPlayable != null,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (listValid) Color(0xFF1E7A3C) else Color(0xFF2A3A4A),
                        contentColor = if (listValid) Color.White else Color(0xFF718096)
                    )
                ) {
                    Text(if (listValid) "\u25B6 JOUER" else "\u25B6 INVALIDE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            })
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
                        Text("$count ${if (game == GameSystem.LEGION_V2) "unités / véhicules" else "vaisseaux"}", color = Color.White)
                    }
                    if (game == GameSystem.ARMADA_V15) {
                        val squadronPts = entries.filter { it.card.kind == CardKind.ARMADA_SQUADRON }.sumOf { it.card.points * it.quantity }
                        LinearProgressIndicator(progress = { (squadronPts.toFloat() / ((limit + 2) / 3).coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Color(0xFFFFC857))
                        Text("Escadrons $squadronPts / ${(limit + 2) / 3} pts", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    } else {
                        val counts = entries.filter { it.card.kind == CardKind.LEGION_UNIT }.groupBy { it.card.legionRank }.mapValues { (_, items) -> items.sumOf { it.quantity } }
                        Text("C ${counts[LegionRank.COMMANDER] ?: 0}/1–2   •   T ${counts[LegionRank.CORPS] ?: 0}/3–6   •   FS ${counts[LegionRank.SPECIAL_FORCES] ?: 0}/0–3", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    }
                    // Indicateur de validité dans l'encadré des points
                    if (entries.isNotEmpty()) {
                        Text(if (listValid) "✓ Liste valide" else "✗ Liste invalide", color = if (listValid) Color(0xFF77D9A7) else Color(0xFFFF927F), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
            }
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("LISTE (${entries.size})") })
                Tab(selectedTab == 1, onClick = { selectedTab = 1 }, text = {
                    if (selectedParent != null) Text("→ ${selectedParent.card.displayName().take(18)}")
                    else Text("CATALOGUE (${additions.size})")
                })
            }
            if (selectedParent != null && selectedTab == 1) {
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFFFFB800).copy(alpha = 0.15f)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ajout à : ${selectedParent.card.displayName()}", color = Color(0xFFFFC857), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { selectedParentId = null }) { Text("✕", color = Color.White) }
                    }
                }
            }
            if (selectedTab == 0) {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    if (entries.isEmpty()) item { Text("Votre force est vide. Ouvrez le catalogue pour ajouter vos premières cartes.", color = Color.LightGray, modifier = Modifier.padding(16.dp)) }
                    // Group entries hierarchically: parents first, then their upgrades indented
                    val parents = entries.filter { it.parentInstanceId == null }
                    val allParentIds = parents.map { it.instanceId }.toSet()
                    val grouped = parents.map { parent ->
                        val children = entries.filter { it.parentInstanceId == parent.instanceId }
                        parent to children
                    }
                    val orphans = entries.filter { it.parentInstanceId != null && it.parentInstanceId !in allParentIds }
                    grouped.forEach { (parent, children) ->
                        item(key = parent.instanceId) {
                            FactionGroup(parent, children, onRemove = { viewModel.remove(it) }, onSelectParent = {
                                if (parent.card.kind == CardKind.LEGION_UNIT || parent.card.kind == CardKind.ARMADA_SHIP) {
                                    selectedParentId = parent.instanceId
                                    selectedTab = 1
                                }
                            })
                        }
                    }
                    orphans.forEach { orphan ->
                        item(key = orphan.instanceId) {
                            BuilderEntryCard(orphan, isChild = true, onRemove = { viewModel.remove(orphan) })
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.weight(1f), placeholder = { Text("Rechercher une carte…") }, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium)
                    // Bouton filtre compact à droite de la barre de recherche
                    Box {
                        Surface(onClick = { filtersMenuOpen = true }, shape = RoundedCornerShape(10.dp), color = if (filterRank != null || filterMaxPts != null || filterKeyword != null) Color(0xFFFFC857) else Color(0xFF2A3A4A)) {
                            Text("\u2699", Modifier.padding(horizontal = 12.dp, vertical = 12.dp), color = if (filterRank != null || filterMaxPts != null || filterKeyword != null) Color(0xFF0A0E15) else Color(0xFFFFC857), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = filtersMenuOpen, onDismissRequest = { filtersMenuOpen = false }) {
                            // Rang/type : petits chips à lettre
                            val ranks = if (game == GameSystem.LEGION_V2) listOf("COMMANDER" to "C", "OPERATIVE" to "O", "CORPS" to "Co", "SPECIAL_FORCES" to "FS", "SUPPORT" to "S", "HEAVY" to "H") else listOf("ARMADA_SHIP" to "V", "ARMADA_SQUADRON" to "E", "COMMANDER" to "Cmd")
                            Text("RANG / TYPE", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ranks.forEach { (r, abbr) ->
                                    FilterChip(selected = filterRank == r, onClick = { filterRank = if (filterRank == r) null else r }, label = { Text(abbr, fontSize = 10.sp) })
                                }
                            }
                            HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 6.dp))
                            Text("POINTS MAX", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(50, 100, 200).forEach { maxPts ->
                                    FilterChip(selected = filterMaxPts == maxPts, onClick = { filterMaxPts = if (filterMaxPts == maxPts) null else maxPts }, label = { Text("≤$maxPts", fontSize = 10.sp) })
                                }
                            }
                            HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 6.dp))
                            OutlinedTextField(value = filterKeyword ?: "", onValueChange = { filterKeyword = it.ifBlank { null } }, modifier = Modifier.padding(horizontal = 12.dp).width(180.dp), placeholder = { Text("Mot-clé (Pierce…)", fontSize = 12.sp) }, singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                            if (filterRank != null || filterMaxPts != null || filterKeyword != null) {
                                TextButton(onClick = { filterRank = null; filterMaxPts = null; filterKeyword = null }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Effacer les filtres", color = Color(0xFFFF927F), style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(additions, key = { it.id }) { card -> CatalogCard(card, entries, selectedParentId, onAdd = { parent, slot -> viewModel.add(card, parent, slot) }) }
                                }
            }
            Text("Hors ligne • catalogue sous réserve des mises à jour officielles", Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp), color = Color(0xFF718096), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FactionCardColor(factionId: String?): Color = when (factionId) {
    "rebel" -> Color(0xFF4EC9E0)        // cyan
    "empire" -> Color(0xFFFF5A5A)       // rouge impérial
    "republic", "republics" -> Color(0xFFE8B54E) // or/jaune
    "separatist", "separatists" -> Color(0xFF9B6DFF) // violet
    "neutral" -> Color(0xFF9EACBC)      // gris
    else -> Color(0xFF9EACBC)
}

@Composable
private fun FactionGroup(parent: ListEntry, children: List<ListEntry>, onRemove: (ListEntry) -> Unit, onSelectParent: () -> Unit) {
    val isSelectable = parent.card.kind == CardKind.LEGION_UNIT || parent.card.kind == CardKind.ARMADA_SHIP
    val accent = FactionCardColor(parent.card.factionId)
    Column(Modifier.fillMaxWidth().border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(15.dp)).padding(5.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BuilderEntryCard(parent, isChild = false, accentColor = accent, onRemove = { onRemove(parent) }, onSelectParent = onSelectParent, isSelectable = isSelectable)
        children.forEach { child ->
            BuilderEntryCard(child, isChild = true, onRemove = { onRemove(child) })
        }
    }
}

@Composable
private fun BuilderEntryCard(entry: ListEntry, isChild: Boolean = false, accentColor: Color = Color(0xFF9EACBC), isSelectable: Boolean = false, onRemove: () -> Unit, onSelectParent: () -> Unit = {}) {
    Card(Modifier
        .fillMaxWidth()
        .then(if (isSelectable) Modifier.clickable { onSelectParent() } else Modifier)
        .then(if (isChild) Modifier.padding(start = 28.dp) else Modifier),
        shape = RoundedCornerShape(if (isChild) 10.dp else 15.dp),
        colors = CardDefaults.cardColors(containerColor = if (isChild) Color(0xFF1E2A3A) else Color(0xFF18212D))) {
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            CardArtwork(entry.card, Modifier.size(width = 64.dp, height = 88.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.card.displayName(), color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text("${entry.card.points * entry.quantity} pts  •  ${entry.card.legionRank?.name?.replace('_', ' ') ?: entry.card.kind.name.replace('_', ' ')}", color = accentColor, style = MaterialTheme.typography.labelSmall)
                if (entry.parentInstanceId != null) Text("↳ ${entry.chosenSlot?.name?.replace('_', ' ') ?: "amélioration liée"}", color = Color(0xFF77D9A7), style = MaterialTheme.typography.labelSmall)
                if (entry.card.kind == CardKind.LEGION_UNIT || entry.card.kind == CardKind.ARMADA_SHIP) {
                    if (entry.card.allowedUpgradeSlots.isNotEmpty()) Text("Slots : ${entry.card.allowedUpgradeSlots.joinToString { it.name.lowercase().replace('_', ' ') }}", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, maxLines = 2)
                }
            }
            TextButton(onClick = onRemove) { Text("RETIRER", color = Color(0xFFFF927F), style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable
private fun CatalogCard(card: CardDefinition, entries: List<ListEntry>, preselectedParentId: String? = null, onAdd: (String?, ArmadaSlot?) -> Unit) {
    val armadaShip = card.kind == CardKind.ARMADA_SHIP
    val isUpgrade = card.kind == CardKind.LEGION_UPGRADE || card.kind == CardKind.ARMADA_UPGRADE
    val isCommander = card.kind == CardKind.COMMANDER
    val requiresTarget = isUpgrade && !isCommander
    val targetUnits = if (card.kind == CardKind.LEGION_UPGRADE) entries.filter { it.card.kind == CardKind.LEGION_UNIT } else entries.filter { it.card.kind == CardKind.ARMADA_SHIP }
    val eligibleTargets = if (card.kind == CardKind.LEGION_UPGRADE) targetUnits.filter { target -> card.upgradeSlots.firstOrNull()?.let { it in target.card.allowedUpgradeSlots } == true } else if (card.kind == CardKind.ARMADA_UPGRADE) targetUnits.filter { target -> card.upgradeSlots.any { it in target.card.allowedUpgradeSlots } && upgradeFitsShip(card, target.card) } else targetUnits
    val preselectedTarget = eligibleTargets.firstOrNull { it.instanceId == preselectedParentId && it.card.allowedUpgradeSlots.any { slot -> slot in card.upgradeSlots } }
    var targetId by remember(card.id, eligibleTargets.size, preselectedTarget) { mutableStateOf(preselectedTarget?.instanceId ?: eligibleTargets.firstOrNull()?.instanceId) }
    var selectedSlot by remember(card.id, preselectedTarget) { mutableStateOf(
        if (preselectedTarget != null) card.upgradeSlots.firstOrNull { it in preselectedTarget.card.allowedUpgradeSlots } ?: card.upgradeSlots.firstOrNull()
        else card.upgradeSlots.firstOrNull()
    ) }
    val eligibleSlots = if (card.kind == CardKind.ARMADA_UPGRADE) card.upgradeSlots.filter { slot -> eligibleTargets.any { target -> slot in target.card.allowedUpgradeSlots } } else if (card.kind == CardKind.LEGION_UPGRADE) card.upgradeSlots.filter { slot -> eligibleTargets.any { target -> slot in target.card.allowedUpgradeSlots } } else card.upgradeSlots
    val slotCounts = entries.filter { it.parentInstanceId != null && (it.card.kind == CardKind.ARMADA_UPGRADE || it.card.kind == CardKind.LEGION_UPGRADE) }.groupBy { it.parentInstanceId to (it.chosenSlot ?: it.card.upgradeSlots.firstOrNull()) }.mapValues { (_, items) -> items.sumOf { it.quantity } }
    val addingTo = eligibleTargets.firstOrNull { it.instanceId == targetId }
    val validSelection = (!requiresTarget || addingTo != null) && (!isUpgrade || (selectedSlot != null && addingTo != null && selectedSlot in addingTo.card.allowedUpgradeSlots && (slotCounts[targetId to selectedSlot] ?: 0) < addingTo.card.allowedUpgradeSlots.count { it == selectedSlot }))
    val alreadyAdded = entries.any { it.card.id == card.id && (card.unique || card.kind == CardKind.COMMANDER) }
    val compatibleSlotFull = isUpgrade && eligibleSlots.isNotEmpty() && eligibleSlots.all { slot -> eligibleTargets.all { target -> (slotCounts[target.instanceId to slot] ?: 0) >= target.card.allowedUpgradeSlots.count { it == slot } } }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF18212D))) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
            CardArtwork(card, Modifier.size(width = 58.dp, height = 80.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(card.displayName(), color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text("${card.points} pts • ${card.kind.name.replace('_', ' ')}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall)
                Text(card.rulesText.orEmpty().ifBlank { "${card.factionId.replace('-', ' ')} • ${if (card.unique) "Unique" else "Standard"}" }, maxLines = 2, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                if (requiresTarget && eligibleTargets.isNotEmpty()) {
                    var expandedTarget by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expandedTarget = true }) { Text("Pour : ${eligibleTargets.firstOrNull { it.instanceId == targetId }?.card?.name ?: "choisir unité"}") }
                        DropdownMenu(expandedTarget, onDismissRequest = { expandedTarget = false }) {
                            eligibleTargets.forEach { target -> DropdownMenuItem(text = { Text(target.card.displayName()) }, onClick = { targetId = target.instanceId; expandedTarget = false }) }
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
    if (source != null) AsyncImage(model = source, contentDescription = "Visuel de ${card.displayName()}", modifier = modifier, contentScale = ContentScale.Crop)
    else Card(modifier, shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF253344))) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF253344), Color(0xFF111820)))), contentAlignment = Alignment.Center) {
            Text(card.displayName().split(' ').take(2).joinToString("\n"), color = Color(0xFF8494A8), style = MaterialTheme.typography.labelSmall)
        }
    }
}
