package com.legionforge.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.legionforge.app.R
import com.legionforge.app.data.model.*
import com.legionforge.app.data.model.WikiSectionEntity

// ═══════════════════════════════════════════════════════════════
//  MODE JEU CONDENSÉ — philosophie de la maquette HTML
//  Vue repliée : cartes vaisseaux compactes. Clic sur un élément
//  → bottom sheet (par le bas) qui réutilise les composants actuels
//  de CardDetailScreen (mode zoom plein écran conservé).
// ═══════════════════════════════════════════════════════════════

// ── état de jeu par vaisseau (hoisté au niveau écran) ──
internal class ShipPlayState(stats: ArmadaStats?, isHuge: Boolean) {
    var hull by mutableIntStateOf(stats?.hull ?: 15)
    var sF by mutableIntStateOf(stats?.shieldFront ?: 0)
    var sR by mutableIntStateOf(stats?.shieldRear ?: 0)
    var sP by mutableIntStateOf(stats?.shieldPort ?: 0)
    var sS by mutableIntStateOf(stats?.shieldStarboard ?: 0)
    var sP2 by mutableIntStateOf(if (isHuge) (stats?.shieldPortAux ?: 0) else 0)
    var sS2 by mutableIntStateOf(if (isHuge) (stats?.shieldStarboardAux ?: 0) else 0)
    var speed by mutableIntStateOf(2)
        var selectedSpeed by mutableIntStateOf(0)
        val maxSpeed = stats?.maxSpeed ?: 3
        val maxShield = 9
        val speedChart: List<Map<String, Int>> = stats?.speedChart.orEmpty()
    val defTokenNames: List<String> = stats?.defenseTokens.orEmpty().ifEmpty { ArmadaDefenseToken.entries.take(4).map { it.name } }
    val defTokenStates = defTokenNames.mapNotNull { name -> ArmadaDefenseToken.entries.firstOrNull { it.name == name.uppercase() }?.let { it to name } }
    var defTokens by mutableStateOf<Map<String, Boolean>>(defTokenStates.mapIndexed { i, (def, _) -> "${def.name}_$i" to false }.toMap())
    var commandOrder by mutableStateOf<ArmadaCommandOrder?>(null)
    val maxOrderStock = stats?.command ?: 1
    var orderTokens by mutableStateOf<Map<String, Int>>(ArmadaCommandOrder.entries.associate { it.name to 0 })
    var activeCrits by mutableStateOf(listOf<CritCard>())
    var usedUpgrades by mutableStateOf(setOf<String>())
}

// ── contenu du bottom sheet ──
internal sealed class SheetContent {
    data class Matrix(val ship: ListEntry, val state: ShipPlayState) : SheetContent()
    data class Shields(val ship: ListEntry, val state: ShipPlayState) : SheetContent()
    data class Dice(val ship: ListEntry, val state: ShipPlayState) : SheetContent()
    data class Defense(val ship: ListEntry, val state: ShipPlayState) : SheetContent()
    data class Command(val ship: ListEntry, val state: ShipPlayState) : SheetContent()
    data class Orders(val ship: ListEntry, val state: ShipPlayState) : SheetContent()
    data class Effect(val ship: ListEntry, val effect: ActiveEffect) : SheetContent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CondensedPlayScreen(
    entries: List<ListEntry>,
    onBack: () -> Unit,
    onZoom: (String) -> Unit,
    wikiSections: List<WikiSectionEntity> = emptyList(),
    onRuleClick: (WikiSectionEntity) -> Unit = {}
) {
    val ships = entries.filter { it.parentInstanceId == null && it.card.kind == CardKind.ARMADA_SHIP }
    val shipStates = remember(entries) {
        ships.associate { ship ->
            val stats = ArmadaStatsParser.parse(ship.card.shipStats, CardKind.ARMADA_SHIP)
            ship.instanceId to ShipPlayState(stats, stats?.size == "huge")
        }
    }
    var sheet by remember { mutableStateOf<SheetContent?>(null) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.play_mode), color = Color.White) },
            navigationIcon = { TextButton(onClick = onBack) { Text("‹", color = Color.White) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E15))
        )
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ships, key = { it.instanceId }) { ship ->
                val state = shipStates[ship.instanceId] ?: return@items
                val children = entries.filter { it.parentInstanceId == ship.instanceId }
                CondensedShipCard(ship, children, state, onElement = { sheet = it }, onZoom = { onZoom(ship.instanceId) }, wikiSections, onRuleClick)
            }
        }
    }

    sheet?.let { content ->
        ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = Color(0xFF192330)) {
            SheetBody(content, wikiSections, onRuleClick, onClose = { sheet = null })
        }
    }
}

// ═══════════════════  CARTE VAISSEAU CONDENSÉE  ═══════════════════
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CondensedShipCard(
    ship: ListEntry,
    children: List<ListEntry>,
    state: ShipPlayState,
    onElement: (SheetContent) -> Unit,
    onZoom: () -> Unit,
    wikiSections: List<WikiSectionEntity>,
    onRuleClick: (WikiSectionEntity) -> Unit
) {
    val stats = remember(ship.instanceId) { ArmadaStatsParser.parse(ship.card.shipStats, CardKind.ARMADA_SHIP) }
    val isHuge = stats?.size == "huge"
    val accent = FactionCardColor(ship.card.factionId)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // ── header ──
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ship.card.displayName(), color = Color(0xFF4FC3F7), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).clickable { onZoom() })
                if (isHuge) Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF3A2050)) { Text("HUGE", Modifier.padding(horizontal = 6.dp, vertical = 1.dp), color = Color(0xFFD7A6FF), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                Text("${ship.card.points}", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            // ── stats ──
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                StatChip(stringResource(R.string.stat_speed), state.speed, Color(0xFF4FC3F7), Modifier.clickable { onElement(SheetContent.Matrix(ship, state)) })
                StatChip(stringResource(R.string.hull), state.hull, Color(0xFFFF6B6B), Modifier.clickable { onElement(SheetContent.Shields(ship, state)) })
                StatChip("Cmd", stats?.command ?: 1, Color(0xFFFFC857), Modifier.clickable { onElement(SheetContent.Command(ship, state)) })
            }
            // ── cadrans (rectangle unique -> ouvre les cadrans) ──
            Surface(shape = RoundedCornerShape(9.dp), color = Color(0xFF1F2C3D), modifier = Modifier.fillMaxWidth().clickable { onElement(SheetContent.Shields(ship, state)) }) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cadrans", color = Color(0xFF9EACBC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("🛡 ${state.sF}/${state.sP}/${state.sS}/${state.sR}", color = Color(0xFF4FC3F7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("⚄ ${(stats?.attackFront?.sum() ?: 0)}", color = Color(0xFFFF6B6B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            // ── roue de commandement (vrais visuels zoom, direct) ──
                        Text("Roue de commandement", color = Color(0xFF9EACBC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ArmadaCommandOrder.entries.forEach { order ->
                                CommandDialButton(order = order, selected = state.commandOrder == order, onClick = { state.commandOrder = if (state.commandOrder == order) null else order }, modifier = Modifier.weight(1f))
                            }
                        }
                        // ── pions d'ordre (vrais visuels zoom, direct) ──
                        Text("Pions d'ordre", color = Color(0xFF9EACBC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ArmadaCommandOrder.entries.forEach { order ->
                                val count = state.orderTokens[order.name] ?: 0
                                OrderTokenDisc(order, count, onWikiClick = {}, onInc = { if (state.orderTokens.values.sum() < state.maxOrderStock) state.orderTokens = state.orderTokens + (order.name to count + 1) }, onDec = { state.orderTokens = state.orderTokens + (order.name to (count - 1).coerceAtLeast(0)) })
                            }
                        }
            // ── jetons de défense (directs, statut par couleur) ──
            Surface(shape = RoundedCornerShape(9.dp), color = Color(0xFF1F2C3D), modifier = Modifier.fillMaxWidth().clickable { onElement(SheetContent.Defense(ship, state)) }) {
                Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.defense_tokens), color = Color(0xFF9EACBC), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    state.defTokenStates.forEachIndexed { i, (def, _) ->
                        val used = state.defTokens["${def.name}_$i"] ?: false
                        val c = if (used) Color(0xFFFF6B6B) else Color(0xFF77D9A7)
                        Surface(shape = RoundedCornerShape(6.dp), color = c.copy(alpha = 0.15f), border = androidx.compose.foundation.BorderStroke(1.dp, c)) {
                            Text(def.label, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = c, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            // ── améliorations (icône webp + nom) ──
            if (children.isNotEmpty()) {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    children.forEach { child ->
                        val slot = child.chosenSlot ?: child.card.upgradeSlots.firstOrNull()
                        val (badge, badgeColor) = if (slot != null) slotBadge(slot) else ("U" to Color(0xFF4FC3F7))
                        val iconPath = slot?.let { slotIconPath(it) }
                        val effect = ActiveEffect(EffectType.UPGRADE, child.card.displayName(), stripBracketName(child.card.rulesText ?: ""), child.instanceId, used = state.usedUpgrades.contains(child.instanceId), slot = slot, pts = child.card.points)
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = Color(0xFF0F1A28),
                            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f)),
                            modifier = Modifier.clickable { onElement(SheetContent.Effect(ship, effect)) }
                        ) {
                            Row(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (iconPath != null) {
                                    AsyncImage(model = "file:///android_asset/$iconPath", contentDescription = null, modifier = Modifier.size(14.dp))
                                } else {
                                    Text(badge, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(child.card.displayName(), color = if (state.usedUpgrades.contains(child.instanceId)) Color(0xFF5A6A7A) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── losange de bouclier (comme la maquette HTML) ──
@Composable
private fun ShieldDiamond(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val p = Path().apply { moveTo(w/2,0f); lineTo(w,h/2); lineTo(w/2,h); lineTo(0f,h/2); close() }
                drawPath(p, color = color)
            }
            Text("$value", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = Color(0xFF9EACBC), fontSize = 7.sp, fontWeight = FontWeight.Bold)
    }
}

// ═══════════════════  BOTTOM SHEET  ═══════════════════
@Composable
private fun SheetBody(content: SheetContent, wikiSections: List<WikiSectionEntity>, onRuleClick: (WikiSectionEntity) -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (content) {
            is SheetContent.Matrix -> {
                SheetTitle(stringResource(R.string.maneuver_matrix))
                ManeuverMatrix(content.state)
            }
            is SheetContent.Shields -> {
                SheetTitle(stringResource(R.string.shields_hull))
                ShieldsHullSheet(content.ship, content.state)
            }
            is SheetContent.Dice -> {
                SheetTitle("Dés")
                DiceSheet(content.ship, content.state)
            }
            is SheetContent.Defense -> {
                SheetTitle(stringResource(R.string.defense_tokens))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    content.state.defTokenStates.forEachIndexed { i, (def, _) ->
                        val key = "${def.name}_$i"
                        val used = content.state.defTokens[key] ?: false
                        DefenseTokenDisc(def, used, onClick = { content.state.defTokens = content.state.defTokens + (key to !used) }, onWikiClick = { findWikiSection(wikiSections, def.label)?.let(onRuleClick) })
                    }
                }
            }
            is SheetContent.Command -> {
                SheetTitle("Roue de commandement")
                CommandDialCard(
                    selected = content.state.commandOrder,
                    onSelect = { content.state.commandOrder = if (content.state.commandOrder == it) null else it },
                    orderTokens = content.state.orderTokens,
                    maxStock = content.state.maxOrderStock,
                    onIncOrder = { name -> if (content.state.orderTokens.values.sum() < content.state.maxOrderStock) content.state.orderTokens = content.state.orderTokens + (name to (content.state.orderTokens[name] ?: 0) + 1) },
                    onDecOrder = { name -> content.state.orderTokens = content.state.orderTokens + (name to ((content.state.orderTokens[name] ?: 0) - 1).coerceAtLeast(0)) },
                    wikiSections = wikiSections,
                    onRuleClick = onRuleClick
                )
            }
            is SheetContent.Orders -> {
                SheetTitle("Jetons")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArmadaCommandOrder.entries.forEach { order ->
                        val count = content.state.orderTokens[order.name] ?: 0
                        OrderTokenDisc(order, count, onWikiClick = { findWikiSection(wikiSections, order.label)?.let(onRuleClick) },
                            onInc = { if (content.state.orderTokens.values.sum() < content.state.maxOrderStock) content.state.orderTokens = content.state.orderTokens + (order.name to count + 1) },
                            onDec = { content.state.orderTokens = content.state.orderTokens + (order.name to (count - 1).coerceAtLeast(0)) })
                    }
                }
            }
            is SheetContent.Effect -> {
                SheetTitle(content.effect.label)
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color(0xFF0A0E15)) {
                    Text(content.effect.desc, Modifier.padding(12.dp), color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(text, color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

// ── boucliers + coque (cadrans en croix) ──
@Composable
private fun ShieldsHullSheet(ship: ListEntry, state: ShipPlayState) {
    val stats = remember(ship.instanceId) { ArmadaStatsParser.parse(ship.card.shipStats, CardKind.ARMADA_SHIP) }
    val isHuge = stats?.size == "huge"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ArcCadran(ArcPos.ABOVE, listOf(stats?.attackFront), listOf(state.sF), { if (state.sF < state.maxShield) state.sF++ }, { if (state.sF > 0) state.sF-- }, { if (state.sF < state.maxShield) state.sF++ }, { if (state.sF > 0) state.sF-- })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
            ArcCadran(ArcPos.LEFT, if (isHuge) listOf(stats?.attackPort, stats?.attackPortAux) else listOf(stats?.attackPort), if (isHuge) listOf(state.sP, state.sP2) else listOf(state.sP), { if (state.sP < state.maxShield) state.sP++ }, { if (state.sP > 0) state.sP-- }, { if (state.sP2 < state.maxShield) state.sP2++ }, { if (state.sP2 > 0) state.sP2-- })
            ArcCadran(ArcPos.RIGHT, if (isHuge) listOf(stats?.attackStarboard, stats?.attackStarboardAux) else listOf(stats?.attackStarboard), if (isHuge) listOf(state.sS, state.sS2) else listOf(state.sS), { if (state.sS < state.maxShield) state.sS++ }, { if (state.sS > 0) state.sS-- }, { if (state.sS2 < state.maxShield) state.sS2++ }, { if (state.sS2 > 0) state.sS2-- })
        }
        ArcCadran(ArcPos.BELOW, listOf(stats?.attackRear), listOf(state.sR), { if (state.sR < state.maxShield) state.sR++ }, { if (state.sR > 0) state.sR-- }, { if (state.sR < state.maxShield) state.sR++ }, { if (state.sR > 0) state.sR-- })
        HorizontalDivider(color = Color(0xFF2A3A4A))
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            BigCounter(stringResource(R.string.stat_speed), state.speed, state.maxSpeed, Color(0xFF77D9A7), { if (state.speed < state.maxSpeed) state.speed++ }, { if (state.speed > 1) state.speed-- })
            Spacer(Modifier.width(24.dp))
            BigCounter(stringResource(R.string.hull), state.hull, stats?.hull ?: 15, Color(0xFFFF6B6B), { if (state.hull < (stats?.hull ?: 15)) state.hull++ }, { if (state.hull > 0) state.hull-- })
        }
        if (state.hull < (stats?.hull ?: 15)) HealthBar(state.hull.toFloat() / (stats?.hull ?: 15), state.hull, stats?.hull ?: 15)
    }
}

// ── dés d'attaque par arc ──
@Composable
private fun DiceSheet(ship: ListEntry, state: ShipPlayState) {
    val stats = remember(ship.instanceId) { ArmadaStatsParser.parse(ship.card.shipStats, CardKind.ARMADA_SHIP) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DiceArcRow("Avant", stats?.attackFront)
        DiceArcRow("Bâbord", stats?.attackPort)
        DiceArcRow("Tribord", stats?.attackStarboard)
        DiceArcRow("Arrière", stats?.attackRear)
    }
}

@Composable
private fun DiceArcRow(label: String, dice: List<Int>?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color(0xFF9EACBC), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
        AttackDiceRow(dice, vertical = false)
    }
}

// ── matrice de manœuvres (triangulaire, comme l'app) ──
@Composable
private fun ManeuverMatrix(state: ShipPlayState) {
    val maxSpeed = state.maxSpeed
    val chart = state.speedChart
    val maxPos = chart.size.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // header vitesses
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(Modifier.width(30.dp))
            (1..maxSpeed).forEach { v ->
                val isSel = v == state.selectedSpeed
                Surface(onClick = { state.selectedSpeed = if (state.selectedSpeed == v) 0 else v }, shape = RoundedCornerShape(6.dp), color = if (isSel) Color(0xFFFFC857) else Color(0xFF2A3A4A), modifier = Modifier.weight(1f)) {
                    Box(Modifier.padding(vertical = 4.dp), contentAlignment = Alignment.Center) { Text("V$v", color = if (isSel) Color(0xFF0A0E15) else Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        // lignes positions (inversées : max en haut)
        chart.reversed().forEachIndexed { revIdx, posMap ->
            val posLabel = maxPos - revIdx
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) { Text("$posLabel", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                (1..maxSpeed).forEach { v ->
                    val n = posMap[v.toString()] ?: -1
                    val cellColor = if (n < 0) Color(0xFF800000) else Color.White
                    val cellText = when { n < 0 -> ""; n == 0 -> "-"; n == 1 -> "I"; n == 2 -> "II"; else -> "III" }
                    val isSel = v == state.selectedSpeed
                    Surface(shape = RoundedCornerShape(6.dp), color = cellColor, modifier = Modifier.weight(1f).height(32.dp), border = if (isSel) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFC857)) else null) {
                        Box(contentAlignment = Alignment.Center) { Text(cellText, color = if (n < 0) Color(0xFF800000) else Color(0xFF1B2B4B), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
