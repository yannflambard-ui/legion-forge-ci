package com.legionforge.app.ui.screens

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

enum class ArmadaDefenseToken(val label: String, val icon: String) {
    BRACE("Brace", "\uD83D\uDEE1"),
    REDIRECT("Redirect", "\u21C4"),
    EVADE("Evade", "\u21BA"),
    SCATTER("Scatter", "\u2601"),
    CONTAIN("Contain", "\u26D4")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardDetailScreen(
    entries: List<ListEntry>,
    initialIndex: Int = 0,
    onBack: () -> Unit
) {
    val playable = entries.filter { e ->
        e.parentInstanceId == null && (
            e.card.kind == CardKind.LEGION_UNIT ||
            e.card.kind == CardKind.ARMADA_SHIP ||
            e.card.kind == CardKind.ARMADA_SQUADRON ||
            e.card.kind == CardKind.COMMANDER
        )
    }
    val safeIndex = initialIndex.coerceIn(0, (playable.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(pageCount = { playable.size.coerceAtLeast(1) }, initialPage = safeIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (playable.isNotEmpty()) playable[pagerState.currentPage].card.name else "Mode partie", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E15))
            )
        }
    ) { pad ->
        if (playable.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("Ajoutez des unites pour utiliser le mode partie", color = Color.Gray)
            }
            return@Scaffold
        }
        Box(Modifier.fillMaxSize().padding(pad)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val unit = playable[page]
                val children = entries.filter { it.parentInstanceId == unit.instanceId }
                when (unit.card.kind) {
                    CardKind.ARMADA_SHIP -> ArmadaShipPage(unit, children, entries)
                    CardKind.ARMADA_SQUADRON -> ArmadaSquadronPage(unit)
                    else -> LegionUnitPage(unit, children)
                }
            }
            if (playable.size > 1) {
                Row(Modifier.fillMaxWidth().padding(bottom = 50.dp).align(Alignment.BottomCenter), horizontalArrangement = Arrangement.Center) {
                    repeat(playable.size) { i ->
                        Box(Modifier.padding(3.dp).size(if (i == pagerState.currentPage) 10.dp else 7.dp).clip(CircleShape)
                            .background(if (i == pagerState.currentPage) Color(0xFFFFC857) else Color(0xFF3A4A5A)))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegionUnitPage(unit: ListEntry, children: List<ListEntry>) {
    val totalPts = unit.card.points + children.sumOf { it.card.points * it.quantity }
    var wounds by remember(unit.instanceId) { mutableIntStateOf(0) }
    var tokens by remember(unit.instanceId) { mutableStateOf(listOf<String>()) }
    val maxHp = 12

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Card block
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(unit.card.name, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        val kind = unit.card.legionRank?.name?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Unite"
                        Text("$kind  •  ${unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelLarge)
                    }
                    Text("${unit.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
                }
                if (!unit.card.rulesText.isNullOrBlank()) Text(unit.card.rulesText, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 6)
                if (unit.card.allowedUpgradeSlots.isNotEmpty()) Text("Slots: ${unit.card.allowedUpgradeSlots.joinToString { it.name.lowercase().replace('_', ' ') }}", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
                if (children.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 4.dp))
                    children.forEach { c ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(modifier = Modifier.weight(1f)) { Text("+ ", color = Color(0xFF77D9A7), fontWeight = FontWeight.Bold); Text(c.card.name, color = Color.White, style = MaterialTheme.typography.bodyMedium) }
                            Text("${c.card.points}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    HorizontalDivider(color = Color(0xFF2A3A4A))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text("Total $totalPts pts", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold) }
                }
            }
        }
        // Tracking
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("SUIVI DE PARTIE", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    BigCounter("BLESSURES", wounds, maxHp, Color(0xFFFF6B6B), { if (wounds < maxHp) wounds++ }, { if (wounds > 0) wounds-- })
                }
                if (wounds > 0) {
                    val ratio = (1f - wounds.toFloat() / maxHp).coerceIn(0f, 1f)
                    val barColor = if (ratio > 0.66f) Color(0xFF77D9A7) else if (ratio > 0.33f) Color(0xFFFFC857) else Color(0xFFFF6B6B)
                    LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = barColor, trackColor = Color(0xFF2A3A4A))
                    Text("${maxHp - wounds} / $maxHp", color = barColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                }
                TokenSection(tokens, onAdd = { tokens = tokens + it }, onRemove = { tokens = tokens - it })
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ArmadaShipPage(unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>) {
    val totalPts = unit.card.points + children.sumOf { it.card.points * it.quantity }
    var hull by remember(unit.instanceId) { mutableIntStateOf(0) }
    var shieldsFront by remember(unit.instanceId) { mutableIntStateOf(0) }
    var shieldsRear by remember(unit.instanceId) { mutableIntStateOf(0) }
    var shieldsPort by remember(unit.instanceId) { mutableIntStateOf(0) }
    var shieldsStarboard by remember(unit.instanceId) { mutableIntStateOf(0) }
    var tokens by remember(unit.instanceId) { mutableStateOf(listOf<String>()) }
    // Defense tokens: each is a pair (name, used=true/false)
    val defTokenNames = remember { ArmadaDefenseToken.entries.take(4) }
    var defTokens by remember(unit.instanceId) { mutableStateOf(defTokenNames.associate { it.name to false }) }

    // Detect commander among all entries for this fleet
    val commander = allEntries.firstOrNull { it.card.kind == CardKind.COMMANDER || (it.card.kind == CardKind.ARMADA_UPGRADE && ArmadaSlot.COMMANDER in it.card.upgradeSlots) }

    val maxHp = 15
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Ship card
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(unit.card.name, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text("Vaisseau  •  ${unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelLarge)
                    }
                    Text("${unit.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
                }
                if (!unit.card.rulesText.isNullOrBlank()) Text(unit.card.rulesText, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 6)

                // Commander
                if (commander != null) {
                    HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.weight(1f)) {
                            Text("\uD83C\uDFC6 ", color = Color(0xFFFFC857))
                            Text(commander.card.name, color = Color(0xFFFFC857), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("${commander.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Upgrades on this ship
                if (children.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 4.dp))
                    Text("Ameliorations", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    children.forEach { c ->
                        val slotIcon = c.chosenSlot?.name?.replace('_', ' ')?.lowercase()?.take(6) ?: "slot"
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(modifier = Modifier.weight(1f)) { Text("+ ", color = Color(0xFF77D9A7), fontWeight = FontWeight.Bold); Text(c.card.name, color = Color.White, style = MaterialTheme.typography.bodyMedium) }
                            Text("${c.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text("Total $totalPts pts", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold) }
            }
        }

        // Defense tokens (green/red)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("JETONS DE DEFENSE", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    defTokens.forEach { (name, used) ->
                        val def = ArmadaDefenseToken.entries.firstOrNull { it.name == name } ?: return@forEach
                        val bgColor = if (used) Color(0xFF5A2020) else Color(0xFF1A4A2A)
                        val fgColor = if (used) Color(0xFFFF6B6B) else Color(0xFF77D9A7)
                        Surface(
                            onClick = { defTokens = defTokens + (name to !used) },
                            shape = RoundedCornerShape(14.dp),
                            color = bgColor,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(def.icon, fontSize = 22.sp)
                                Text(def.label, color = fgColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(if (used) "UTILISE" else "PRET", color = fgColor.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Shields diamond
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("BOUCLIERS & COQUE", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                // Shield diamond
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MiniShield("AV", shieldsFront, { if (shieldsFront < 9) shieldsFront++ }, { if (shieldsFront > 0) shieldsFront-- })
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        MiniShield("BAB", shieldsPort, { if (shieldsPort < 9) shieldsPort++ }, { if (shieldsPort > 0) shieldsPort-- })
                        MiniShield("TRIB", shieldsStarboard, { if (shieldsStarboard < 9) shieldsStarboard++ }, { if (shieldsStarboard > 0) shieldsStarboard-- })
                    }
                    MiniShield("ARR", shieldsRear, { if (shieldsRear < 9) shieldsRear++ }, { if (shieldsRear > 0) shieldsRear-- })
                }
                HorizontalDivider(color = Color(0xFF2A3A4A))
                // Hull
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    BigCounter("COQUE", hull, maxHp, Color(0xFFFF6B6B), { if (hull < maxHp) hull++ }, { if (hull > 0) hull-- })
                }
                if (hull > 0) {
                    val ratio = (1f - hull.toFloat() / maxHp).coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (ratio > 0.66f) Color(0xFF77D9A7) else if (ratio > 0.33f) Color(0xFFFFC857) else Color(0xFFFF6B6B),
                        trackColor = Color(0xFF2A3A4A))
                    Text("${maxHp - hull} / $maxHp", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                }
            }
        }

        // Generic tokens
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TokenSection(tokens, onAdd = { tokens = tokens + it }, onRemove = { tokens = tokens - it })
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ArmadaSquadronPage(unit: ListEntry) {
    var hull by remember(unit.instanceId) { mutableIntStateOf(0) }
    var tokens by remember(unit.instanceId) { mutableStateOf(listOf<String>()) }
    val maxHp = 8

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(unit.card.name, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Text("Escadron  •  ${unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelLarge)
                    }
                    Text("${unit.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
                }
                if (!unit.card.rulesText.isNullOrBlank()) Text(unit.card.rulesText, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 4)
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SUIVI ESCADRON", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    BigCounter("COQUE / BLESSURES", hull, maxHp, Color(0xFFFF6B6B), { if (hull < maxHp) hull++ }, { if (hull > 0) hull-- })
                }
                if (hull > 0) {
                    val ratio = (1f - hull.toFloat() / maxHp).coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (ratio > 0.66f) Color(0xFF77D9A7) else Color(0xFFFFC857), trackColor = Color(0xFF2A3A4A))
                }
                TokenSection(tokens, { tokens = tokens + it }, { tokens = tokens - it })
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TokenSection(tokens: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("JETONS / MARQUEURS", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Dgt" to Color(0xFFFF6B6B), "Etat" to Color(0xFFFFC857), "Bcl" to Color(0xFF4FC3F7), "Ordre" to Color(0xFF77D9A7))
                .forEach { (label, color) ->
                    Surface(onClick = { onAdd(label) }, shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.2f)) {
                        Text(label, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
        }
        if (tokens.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tokens.forEach { t ->
                    Surface(onClick = { onRemove(t) }, shape = RoundedCornerShape(8.dp), color = Color(0xFF3B2224)) {
                        Text("$t  \u2715", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFFFFC7B7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text("Appuyez sur un jeton pour le retirer", color = Color(0xFF5A6A7A), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MiniShield(label: String, value: Int, onInc: () -> Unit, onDec: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(30.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", color = Color(0xFF4FC3F7), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(40.dp))
            Text(label, color = Color(0xFF9EACBC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(30.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun BigCounter(label: String, value: Int, max: Int, color: Color, onInc: () -> Unit, onDec: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }
            Text("$value", color = color, fontSize = 44.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(70.dp))
            Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}