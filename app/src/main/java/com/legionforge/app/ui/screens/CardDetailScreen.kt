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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legionforge.app.data.model.*

/**
 * Game mode: swipeable card carousel.
 * Top = card + upgrades. Bottom = wounds, shields, tokens (table-ready).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardDetailScreen(
    entries: List<ListEntry>,
    initialIndex: Int = 0,
    onBack: () -> Unit
) {
    // Only show "playable" cards: units, ships, commanders
    val playable = entries.filter { e ->
        e.parentInstanceId == null && (
            e.card.kind == CardKind.LEGION_UNIT ||
            e.card.kind == CardKind.ARMADA_SHIP ||
            e.card.kind == CardKind.COMMANDER
        )
    }
    val safeIndex = initialIndex.coerceIn(0, (playable.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(pageCount = { playable.size.coerceAtLeast(1) }, initialPage = safeIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (playable.isNotEmpty()) "${playable[pagerState.currentPage].card.name}" else "Mode partie",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(pad)
        ) { page ->
            val unit = playable[page]
            val children = entries.filter { it.parentInstanceId == unit.instanceId }
            GameCardPage(unit, children)
        }

        // Page indicator dots
        if (playable.size > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 50.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(playable.size) { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        Modifier
                            .padding(3.dp)
                            .size(if (selected) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color(0xFFFFC857) else Color(0xFF3A4A5A))
                    )
                }
            }
        }
    }
}

@Composable
private fun GameCardPage(unit: ListEntry, children: List<ListEntry>) {
    val totalPoints = unit.card.points + children.sumOf { it.card.points * it.quantity }
    var wounds by remember(unit.instanceId) { mutableIntStateOf(0) }
    var shields by remember(unit.instanceId) { mutableIntStateOf(0) }
    var tokens by remember(unit.instanceId) { mutableStateOf(listOf<String>()) }

    val isShip = unit.card.kind == CardKind.ARMADA_SHIP
    // Estimate max HP from rulesText or sensible default
    val maxHp = 15

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E15))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // === TOP HALF: CARD ===
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Title row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(unit.card.name, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        val kindDisplay = unit.card.legionRank?.name?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() }
                            ?: unit.card.kind.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                        Text("$kindDisplay  •  ${unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelLarge)
                    }
                    Text("${unit.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
                }

                // Rules
                if (!unit.card.rulesText.isNullOrBlank()) {
                    Text(unit.card.rulesText, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 6)
                }

                // Slots
                if (unit.card.allowedUpgradeSlots.isNotEmpty()) {
                    Text(
                        "Slots: ${unit.card.allowedUpgradeSlots.groupingBy { it }.eachCount().entries.joinToString { "${it.value}x ${it.key.name.lowercase().replace('_', ' ')}" }}",
                        color = Color(0xFF9EACBC),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Upgrades attached
                if (children.isNotEmpty()) {
                    Divider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 4.dp))
                    children.forEach { child ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("+ ", color = Color(0xFF77D9A7), fontWeight = FontWeight.Bold)
                                Text(child.card.name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("${child.card.points}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Divider(color = Color(0xFF2A3A4A))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("Total $totalPoints pts", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // === BOTTOM HALF: STATS & TRACKING ===
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("SUIVI DE PARTIE", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                // Health row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BigCounter(
                        label = if (isShip) "COQUE" else "BLESSURES",
                        value = wounds,
                        max = maxHp,
                        color = Color(0xFFFF6B6B),
                        onInc = { if (wounds < maxHp) wounds++ },
                        onDec = { if (wounds > 0) wounds-- }
                    )
                    if (isShip) {
                        BigCounter(
                            label = "BOUCLIERS",
                            value = shields,
                            max = maxHp,
                            color = Color(0xFF4FC3F7),
                            onInc = { if (shields < maxHp) shields++ },
                            onDec = { if (shields > 0) shields-- }
                        )
                    }
                }

                // Health bar
                if (wounds > 0) {
                    val ratio = (1f - wounds.toFloat() / maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
                    val barColor = when {
                        ratio > 0.66f -> Color(0xFF77D9A7)
                        ratio > 0.33f -> Color(0xFFFFC857)
                        else -> Color(0xFFFF6B6B)
                    }
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = barColor,
                        trackColor = Color(0xFF2A3A4A)
                    )
                    Text("${maxHp - wounds} / $maxHp", color = barColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                }

                // Quick token buttons
                Text("JETONS", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Dgt" to Color(0xFFFF6B6B),
                        "Etat" to Color(0xFFFFC857),
                        "Bcl" to Color(0xFF4FC3F7),
                        "Ordre" to Color(0xFF77D9A7)
                    ).forEach { (label, color) ->
                        Surface(
                            onClick = { tokens = tokens + label },
                            shape = RoundedCornerShape(12.dp),
                            color = color.copy(alpha = 0.2f),
                            border = null
                        ) {
                            Text(label, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Active tokens
                if (tokens.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tokens.forEach { token ->
                            Surface(
                                onClick = { tokens = tokens - token },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF3B2224)
                            ) {
                                Text("$token  ✕", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFFFFC7B7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Tap-to-heal hint
                Text("Appuyez sur un jeton pour le retirer", color = Color(0xFF5A6A7A), style = MaterialTheme.typography.labelSmall)
            }
        }

        // Spacer for nav dots
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun BigCounter(
    label: String,
    value: Int,
    max: Int,
    color: Color,
    onInc: () -> Unit,
    onDec: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Minus button
            Surface(
                onClick = onDec,
                shape = CircleShape,
                color = Color(0xFF2A3A4A),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Value
            Text(
                "$value",
                color = color,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(70.dp)
            )
            // Plus button
            Surface(
                onClick = onInc,
                shape = CircleShape,
                color = Color(0xFF2A3A4A),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}