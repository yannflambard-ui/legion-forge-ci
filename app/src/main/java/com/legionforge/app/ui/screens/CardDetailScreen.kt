package com.legionforge.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.legionforge.app.data.model.*

/**
 * Generic card detail / game-mode view.
 * Shows a unit/ship with all its upgrades, plus live wound/shield tracking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    unit: ListEntry,
    entries: List<ListEntry>,
    onBack: () -> Unit
) {
    val children = entries.filter { it.parentInstanceId == unit.instanceId }
    val totalPoints = unit.card.points + children.sumOf { it.card.points * it.quantity }

    // Live tracking state
    var wounds by remember { mutableIntStateOf(0) }
    var shields by remember { mutableIntStateOf(0) }
    var tokens by remember { mutableStateOf(listOf<String>()) }
    var tokenInput by remember { mutableStateOf("") }

    val isLegion = unit.card.gameSystem == GameSystem.LEGION_V2
    val isShip = unit.card.kind == CardKind.ARMADA_SHIP

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(unit.card.name, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { TextButton(onClick = onBack) { Text("< Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E15))
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0E15))
                .padding(pad)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            // === CARD HEADER ===
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Title row
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(unit.card.name, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                            Text("${unit.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
                        }
                        // Type + faction
                        val kindLabel = unit.card.kind.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                        val rankLabel = unit.card.legionRank?.name?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() }
                        Text(
                            (rankLabel ?: kindLabel) + "  •  " + unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() },
                            color = Color(0xFFFFC857),
                            style = MaterialTheme.typography.labelLarge
                        )
                        // Rules text
                        if (!unit.card.rulesText.isNullOrBlank()) {
                            Text(unit.card.rulesText, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                        }
                        // Slots
                        if (unit.card.allowedUpgradeSlots.isNotEmpty()) {
                            Text(
                                "Slots: ${unit.card.allowedUpgradeSlots.joinToString { it.name.lowercase().replace('_', ' ') }}",
                                color = Color(0xFF9EACBC),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // === UPGRADES ATTACHED ===
            if (children.isNotEmpty()) {
                item {
                    Text(
                        "Améliorations (${children.size})",
                        color = Color(0xFFFFC857),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                items(children, key = { it.instanceId }) { child ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2A3A))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(child.card.name, color = Color.White, style = MaterialTheme.typography.titleSmall)
                                val slotLabel = child.chosenSlot?.name?.replace('_', ' ')?.lowercase() ?: "—"
                                Text("↳ $slotLabel • ${child.card.points} pts", color = Color(0xFF77D9A7), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // === STATS TRACKER ===
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Mode partie • suivi en direct", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall)

                        // WOUNDS / HULL tracker
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatCounter(
                                label = if (isShip) "Coque" else "Blessures",
                                value = wounds,
                                color = Color(0xFFFF6B6B),
                                onInc = { if (wounds < 20) wounds++ },
                                onDec = { if (wounds > 0) wounds-- }
                            )

                            // SHIELDS (Armada)
                            if (isShip) {
                                StatCounter(
                                    label = "Boucliers",
                                    value = shields,
                                    color = Color(0xFF4FC3F7),
                                    onInc = { if (shields < 20) shields++ },
                                    onDec = { if (shields > 0) shields-- }
                                )
                            }
                        }

                        // Health bar visualization
                        if (wounds > 0) {
                            val healthColor = when {
                                wounds <= 2 -> Color(0xFF77D9A7)
                                wounds <= 5 -> Color(0xFFFFC857)
                                else -> Color(0xFFFF6B6B)
                            }
                            LinearProgressIndicator(
                                progress = { ((20 - wounds).toFloat() / 20).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = healthColor,
                                trackColor = Color(0xFF2A3A4A)
                            )
                        }

                        // TOKENS tracker (generic)
                        Text("Jetons / marqueurs", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelLarge)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("🔴 Dégât", "🟡 État", "🔵 Bouclier", "⚪ Autre").forEach { tag ->
                                Surface(
                                    Modifier.clickable { tokens = tokens + tag.split(" ").last() },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF253344)
                                ) {
                                    Text(tag, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                        if (tokens.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                tokens.forEach { token ->
                                    Surface(
                                        Modifier.clickable { tokens = tokens - token },
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF3B2224)
                                    ) {
                                        Text("$token ✕", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFFFFC7B7), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // === POINTS SUMMARY ===
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total points", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Text("$totalPoints pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCounter(
    label: String,
    value: Int,
    color: Color,
    onInc: () -> Unit,
    onDec: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = onDec,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2A3A4A))
            ) {
                Text("-", color = Color.White, fontSize = 18.sp)
            }
            Text(
                "$value",
                color = color,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(50.dp)
            )
            IconButton(
                onClick = onInc,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF2A3A4A))
            ) {
                Text("+", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}