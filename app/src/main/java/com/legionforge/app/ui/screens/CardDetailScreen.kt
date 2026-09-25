package com.legionforge.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legionforge.app.data.model.*

// ── common crit cards ───────────────────────────────────
data class CritCard(val name: String, val effect: String)
private val armadaCrits = listOf(
    CritCard("Blinded Laser", "Le vaisseau ne peut pas utiliser l'armement principal ce tour."),
    CritCard("Burning Sensors", "Le vaisseau saute son activation et gagne 1 jeton d'état."),
    CritCard("Damaged Control", "Le vaisseau ne peut pas utiliser son jeton de commandement suivant."),
    CritCard("Damaged Weapons", "Les dés rouges du vaisseau sont relancés. Piochez une nouvelle carte dégâts."),
    CritCard("Engines Damaged", "Le vaisseau ne peut pas se déplacer de sa vitesse maximale."),
    CritCard("Hull Breach", "Ignorez les boucliers sur les dés de dégâts suivants."),
    CritCard("Shaken Crew", "Le vaisseau saute son activation."),
    CritCard("Structural Damage", "Tous les dégâts subis par la coque sont doublés ce tour.")
)
private val legionCrits = listOf(
    CritCard("Blessure critique", "La figurine subit une blessure negligee."),
    CritCard("Sonne", "La figurine ne peut pas effectuer d'action ce tour."),
    CritCard("Desequilibre", "Retirez un de de la reserve de des."),
    CritCard("Statut altere", "Appliquez un marqueur d'etat a la figurine.")
)

// ── defence tokens for Armada ────────────────────────────
enum class ArmadaDefenseToken(val label: String, val icon: String) {
    BRACE("Brace", "\uD83D\uDEE1"),
    REDIRECT("Redirect", "\u21C4"),
    EVADE("Evade", "\u21BA"),
    SCATTER("Scatter", "\u2601"),
    CONTAIN("Contain", "\u26D4")
}

// ── active effect model ─────────────────────────────────
private enum class EffectType { COMMANDER, UPGRADE, CRIT, ABILITY }
private data class ActiveEffect(val type: EffectType, val label: String, val desc: String, val id: String, val used: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardDetailScreen(entries: List<ListEntry>, initialIndex: Int = 0, onBack: () -> Unit) {
    val playable = entries.filter { e ->
        e.parentInstanceId == null && (e.card.kind == CardKind.LEGION_UNIT || e.card.kind == CardKind.ARMADA_SHIP || e.card.kind == CardKind.ARMADA_SQUADRON || e.card.kind == CardKind.COMMANDER)
    }
    val safeIndex = initialIndex.coerceIn(0, (playable.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(pageCount = { playable.size.coerceAtLeast(1) }, initialPage = safeIndex)
    var round by remember { mutableIntStateOf(1) }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (playable.isNotEmpty()) playable[pagerState.currentPage].card.name else "Mode partie", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
            actions = {
                Surface(onClick = { if (round > 1) round-- }, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
                Text(" R$round ", color = Color(0xFFFFC857), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Surface(onClick = { round++ }, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E15)))
    }) { pad ->
        if (playable.isEmpty()) { Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { Text("Ajoutez des unites pour utiliser le mode partie", color = Color.Gray) }; return@Scaffold }
        Box(Modifier.fillMaxSize().padding(pad)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val unit = playable[page]; val children = entries.filter { it.parentInstanceId == unit.instanceId }
                when (unit.card.kind) {
                    CardKind.ARMADA_SHIP -> ArmadaShipPage(unit, children, entries)
                    CardKind.ARMADA_SQUADRON -> ArmadaSquadronPage(unit)
                    else -> LegionUnitPage(unit, children, entries)
                }
            }
            if (playable.size > 1) {
                Row(Modifier.fillMaxWidth().padding(bottom = 50.dp).align(Alignment.BottomCenter), horizontalArrangement = Arrangement.Center) {
                    repeat(playable.size) { i -> Box(Modifier.padding(3.dp).size(if (i == pagerState.currentPage) 10.dp else 7.dp).clip(CircleShape).background(if (i == pagerState.currentPage) Color(0xFFFFC857) else Color(0xFF3A4A5A))) }
                }
            }
        }
    }
}

// ═══════════════════  LEGION  ═══════════════════════════
@Composable
private fun LegionUnitPage(unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>) {
    val totalPts = unit.card.points + children.sumOf { it.card.points * it.quantity }
    var wounds by remember(unit.instanceId) { mutableIntStateOf(0) }
    var tokens by remember(unit.instanceId) { mutableStateOf(listOf<String>()) }
    var activeCrits by remember(unit.instanceId) { mutableStateOf(listOf<CritCard>()) }
    var usedUpgrades by remember(unit.instanceId) { mutableStateOf(setOf<String>()) }
    val maxHp = 12
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardBlock(unit, children, totalPts)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("SUIVI", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { BigCounter("BLESSURES", wounds, maxHp, Color(0xFFFF6B6B), { if (wounds < maxHp) wounds++ }, { if (wounds > 0) wounds-- }) }
                if (wounds > 0) { val r = (1f - wounds.toFloat() / maxHp).coerceIn(0f, 1f); HealthBar(r, maxHp - wounds, maxHp) }
                TokenSection(tokens, { tokens = tokens + it }, { tokens = tokens - it })
            }
        }
        EffectsPanel(
            effects = buildList {
                children.forEach { c -> add(ActiveEffect(EffectType.UPGRADE, c.card.name, c.card.rulesText ?: "Amelioration installee", c.instanceId).copy(used = usedUpgrades.contains(c.instanceId))) }
                activeCrits.forEach { c -> add(ActiveEffect(EffectType.CRIT, c.name, c.effect, c.name)) }
            },
            critSelector = { expanded, onDismiss, onSelect ->
                CritSelectorDropdown(legionCrits, expanded, onDismiss, onSelect)
            },
            onAddCrit = { activeCrits = activeCrits + it },
            onRemoveCrit = { activeCrits = activeCrits - it },
            allCrits = legionCrits
        )
        Spacer(Modifier.height(20.dp))
    }
}

// ═══════════════════  ARMADA SHIP  ═══════════════════════
@Composable
private fun ArmadaShipPage(unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>) {
    val totalPts = unit.card.points + children.sumOf { it.card.points * it.quantity }
    var hull by remember(unit.instanceId) { mutableIntStateOf(0) }
    var sF by remember(unit.instanceId) { mutableIntStateOf(0) }; var sR by remember(unit.instanceId) { mutableIntStateOf(0) }
    var sP by remember(unit.instanceId) { mutableIntStateOf(0) }; var sS by remember(unit.instanceId) { mutableIntStateOf(0) }
    var tokens by remember(unit.instanceId) { mutableStateOf(listOf<String>()) }
    var activeCrits by remember(unit.instanceId) { mutableStateOf(listOf<CritCard>()) }
    var usedUpgrades by remember(unit.instanceId) { mutableStateOf(setOf<String>()) }
    val defTokenNames = remember { ArmadaDefenseToken.entries.take(4) }
    var defTokens by remember(unit.instanceId) { mutableStateOf<Map<String, Boolean>>(defTokenNames.associate { it.name to false }) }
    val commander = allEntries.firstOrNull { it.card.kind == CardKind.COMMANDER || (it.card.kind == CardKind.ARMADA_UPGRADE && ArmadaSlot.COMMANDER in it.card.upgradeSlots) }
    val maxHp = 15

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ── ship card ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) { Text(unit.card.name, color = Color.White, style = MaterialTheme.typography.headlineSmall); Text("Vaisseau  •  ${unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelLarge) }
                    Text("${unit.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
                }
                if (!unit.card.rulesText.isNullOrBlank()) Text(unit.card.rulesText, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 6)
                if (commander != null) {
                    val used = usedUpgrades.contains("cmd")
                    HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 2.dp))
                    Row(Modifier.fillMaxWidth().clickable { usedUpgrades = if (used) usedUpgrades - "cmd" else usedUpgrades + "cmd" }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.weight(1f)) {
                            Text(if (used) "\u25CB " else "\u25C9 ", color = if (used) Color(0xFF5A6A7A) else Color(0xFFFFC857))
                            Text(commander.card.name, color = if (used) Color(0xFF5A6A7A) else Color(0xFFFFC857), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("${commander.card.points} pts", color = if (used) Color(0xFF5A6A7A) else Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (children.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 4.dp))
                    Text("Ameliorations  (cliquez pour activer/désactiver)", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    children.forEach { c ->
                        val used = usedUpgrades.contains(c.instanceId)
                        Row(Modifier.fillMaxWidth().clickable { usedUpgrades = if (used) usedUpgrades - c.instanceId else usedUpgrades + c.instanceId }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(modifier = Modifier.weight(1f)) {
                                Text(if (used) "\u25CB " else "\u25C9 ", color = if (used) Color(0xFF5A6A7A) else Color(0xFF77D9A7))
                                Text(c.card.name, color = if (used) Color(0xFF5A6A7A) else Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("${c.card.points} pts", color = if (used) Color(0xFF5A6A7A) else Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text("Total $totalPts pts", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold) }
            }
        }
        // ── defense tokens ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp)) {
                Text("JETONS DE DEFENSE", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    defTokens.forEach { (name, used) ->
                        val def = ArmadaDefenseToken.entries.firstOrNull { it.name == name } ?: return@forEach
                        Surface(onClick = { defTokens = defTokens + (name to !used) }, shape = RoundedCornerShape(14.dp), color = if (used) Color(0xFF5A2020) else Color(0xFF1A4A2A), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(def.icon, fontSize = 22.sp)
                                Text(def.label, color = if (used) Color(0xFFFF6B6B) else Color(0xFF77D9A7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(if (used) "UTILISE" else "PRET", color = (if (used) Color(0xFFFF6B6B) else Color(0xFF77D9A7)).copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        // ── shields & hull ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("BOUCLIERS & COQUE", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MiniShield("AV", sF, { if (sF < 9) sF++ }, { if (sF > 0) sF-- })
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) { MiniShield("BAB", sP, { if (sP < 9) sP++ }, { if (sP > 0) sP-- }); MiniShield("TRIB", sS, { if (sS < 9) sS++ }, { if (sS > 0) sS-- }) }
                    MiniShield("ARR", sR, { if (sR < 9) sR++ }, { if (sR > 0) sR-- })
                }
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { BigCounter("COQUE", hull, maxHp, Color(0xFFFF6B6B), { if (hull < maxHp) hull++ }, { if (hull > 0) hull-- }) }
                if (hull > 0) { HealthBar((1f - hull.toFloat() / maxHp).coerceIn(0f, 1f), maxHp - hull, maxHp) }
            }
        }
        // ── tokens ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp)) { TokenSection(tokens, { tokens = tokens + it }, { tokens = tokens - it }) }
        }
        // ── effects panel ──
        EffectsPanel(
            effects = buildList {
                if (commander != null) add(ActiveEffect(EffectType.COMMANDER, commander.card.name, commander.card.rulesText ?: "Commandant de la flotte", "cmd_${commander.instanceId}")
                    .copy(used = usedUpgrades.contains("cmd")))
                children.forEach { c -> add(ActiveEffect(EffectType.UPGRADE, c.card.name, c.card.rulesText ?: "Amelioration installee", c.instanceId)
                    .copy(used = usedUpgrades.contains(c.instanceId))) }
                activeCrits.forEach { c -> add(ActiveEffect(EffectType.CRIT, c.name, c.effect, "crit_${c.name}")) }
            },
            critSelector = { expanded, onDismiss, onSelect ->
                CritSelectorDropdown(armadaCrits, expanded, onDismiss, onSelect)
            },
            onAddCrit = { activeCrits = activeCrits + it },
            onRemoveCrit = { activeCrits = activeCrits - it },
            allCrits = armadaCrits
        )
        Spacer(Modifier.height(20.dp))
    }
}

// ═══════════════════  SQUADRON  ═══════════════════════════
@Composable
private fun ArmadaSquadronPage(unit: ListEntry) {
    var hull by remember(unit.instanceId) { mutableIntStateOf(0) }; var tokens by remember(unit.instanceId) { mutableStateOf(listOf<String>()) }
    val maxHp = 8
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardBlock(unit, emptyList(), unit.card.points)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SUIVI ESCADRON", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { BigCounter("COQUE", hull, maxHp, Color(0xFFFF6B6B), { if (hull < maxHp) hull++ }, { if (hull > 0) hull-- }) }
                if (hull > 0) HealthBar((1f - hull.toFloat() / maxHp).coerceIn(0f, 1f), maxHp - hull, maxHp)
                TokenSection(tokens, { tokens = tokens + it }, { tokens = tokens - it })
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ═══════════════════  SHARED COMPONENTS  ═══════════════════

@Composable
private fun CardBlock(unit: ListEntry, children: List<ListEntry>, totalPts: Int) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(unit.card.name, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    val kind = unit.card.legionRank?.name?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: unit.card.kind.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                    Text("$kind  •  ${unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelLarge)
                }
                Text("${unit.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
            }
            if (!unit.card.rulesText.isNullOrBlank()) Text(unit.card.rulesText, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 6)
            if (children.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 4.dp))
                children.forEach { c -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Row(modifier = Modifier.weight(1f)) { Text("+ ", color = Color(0xFF77D9A7), fontWeight = FontWeight.Bold); Text(c.card.name, color = Color.White, style = MaterialTheme.typography.bodyMedium) }; Text("${c.card.points}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium) } }
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text("Total $totalPts pts", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// Effects panel with crit selector
@Composable
private fun EffectsPanel(
    effects: List<ActiveEffect>,
    critSelector: @Composable (Boolean, () -> Unit, (CritCard) -> Unit) -> Unit,
    onAddCrit: (CritCard) -> Unit,
    onRemoveCrit: (CritCard) -> Unit,
    allCrits: List<CritCard>
) {
    var expandedCrit by remember { mutableStateOf(false) }
    var selectedEffect by remember { mutableStateOf<ActiveEffect?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Header + add crit button
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("EFFETS ACTIFS", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Surface(onClick = { expandedCrit = true }, shape = RoundedCornerShape(10.dp), color = Color(0xFF5A2020)) {
                Text(" + Dgt Crit", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color(0xFFFF6B6B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        // Crit selector dropdown
        critSelector(expandedCrit, { expandedCrit = false }, { c -> onAddCrit(c); expandedCrit = false })

        // Effect list
        if (effects.isEmpty()) {
            Text("Aucun effet actif", color = Color(0xFF5A6A7A), style = MaterialTheme.typography.bodySmall)
        } else {
            effects.forEach { eff ->
                val (icon, iconColor) = when (eff.type) {
                    EffectType.COMMANDER -> "C" to Color(0xFFFFC857)
                    EffectType.UPGRADE -> "U" to Color(0xFF4FC3F7)
                    EffectType.CRIT -> "\u26A1" to Color(0xFFFF6B6B)
                    else -> "?" to Color.Gray
                }
                Surface(
                    onClick = { selectedEffect = if (selectedEffect == eff) null else eff },
                    shape = RoundedCornerShape(12.dp),
                    color = when { eff.used -> Color(0xFF1A1A2A); eff.type == EffectType.CRIT -> Color(0xFF3B2224); eff.type == EffectType.ABILITY -> Color(0xFF1E3A2A); else -> Color(0xFF1E2A3A) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(24.dp).clip(CircleShape).background(if (eff.used) Color(0xFF3A3A4A) else iconColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (eff.used) Color(0xFF5A6A7A) else iconColor) }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(eff.label, color = if (eff.used) Color(0xFF5A6A7A) else Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = if (eff.type == EffectType.CRIT) FontWeight.Bold else FontWeight.Normal)
                                        if (eff.used) { Spacer(Modifier.width(6.dp)); Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF3A3A4A)) { Text("ACTIVEE", Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color(0xFF5A6A7A), fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
                                    }
                                    Text(
                                        when (eff.type) { EffectType.COMMANDER -> "Commandant"; EffectType.UPGRADE -> "Amelioration"; EffectType.CRIT -> "Degat critique"; EffectType.ABILITY -> "Capacite" },
                                        color = (if (eff.used) Color(0xFF5A6A7A) else iconColor).copy(alpha = 0.6f), fontSize = 10.sp
                                    )
                                }
                            }
                            Text(if (selectedEffect == eff) "\u25B2" else "\u25BC", color = Color(0xFF9EACBC), fontSize = 12.sp)
                        }
                        // Expanded description
                        if (selectedEffect == eff) {
                            Spacer(Modifier.height(8.dp))
                            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color(0xFF0A0E15)) {
                                Text(eff.desc, Modifier.padding(12.dp), color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                            }
                            if (eff.type == EffectType.CRIT) {
                                Spacer(Modifier.height(4.dp))
                                TextButton(onClick = { onRemoveCrit(allCrits.firstOrNull { it.name == eff.label } ?: return@TextButton) }) { Text("Retirer le critique", color = Color(0xFFFF6B6B), style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CritSelectorDropdown(crits: List<CritCard>, expanded: Boolean, onDismiss: () -> Unit, onSelect: (CritCard) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        crits.forEach { crit ->
            DropdownMenuItem(text = { Column { Text(crit.name, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(crit.effect.take(60)+"...", fontSize = 11.sp, color = Color.Gray) } }, onClick = { onSelect(crit) })
        }
    }
}

@Composable
private fun HealthBar(ratio: Float, current: Int, max: Int) {
    val barColor = when { ratio > 0.66f -> Color(0xFF77D9A7); ratio > 0.33f -> Color(0xFFFFC857); else -> Color(0xFFFF6B6B) }
    LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = barColor, trackColor = Color(0xFF2A3A4A))
    Box(Modifier.fillMaxWidth()) { Text("$current / $max", color = barColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterEnd)) }
}

@Composable
private fun TokenSection(tokens: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("JETONS / MARQUEURS", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Dgt" to Color(0xFFFF6B6B), "Etat" to Color(0xFFFFC857), "Bcl" to Color(0xFF4FC3F7), "Ordre" to Color(0xFF77D9A7)).forEach { (l, c) ->
                Surface(onClick = { onAdd(l) }, shape = RoundedCornerShape(12.dp), color = c.copy(alpha = 0.2f)) { Text(l, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = c, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
        }
        if (tokens.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tokens.forEach { t -> Surface(onClick = { onRemove(t) }, shape = RoundedCornerShape(8.dp), color = Color(0xFF3B2224)) { Text("$t  \u2715", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFFFFC7B7), fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
            }
        }
        Text("Appuyez sur un jeton pour le retirer", color = Color(0xFF5A6A7A), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MiniShield(label: String, value: Int, onInc: () -> Unit, onDec: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(30.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", color = Color(0xFF4FC3F7), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(40.dp))
            Text(label, color = Color(0xFF9EACBC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(30.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
    }
}

@Composable
private fun BigCounter(label: String, value: Int, max: Int, color: Color, onInc: () -> Unit, onDec: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) } }
            Text("$value", color = color, fontSize = 44.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(70.dp))
            Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) } }
        }
    }
}