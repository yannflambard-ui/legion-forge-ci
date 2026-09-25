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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
        TopAppBar(title = { Text(if (playable.isNotEmpty()) playable[pagerState.currentPage].card.displayName() else "Mode partie", style = MaterialTheme.typography.titleMedium) },
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
                    // Degats critiques reserves aux vaisseaux capitaux (par Regle Armada). Un commandant est equipe sur un vaisseau, il n'a pas de page de degats propres.
                    CardKind.COMMANDER -> CommanderPage(unit)
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
// Stats réels des cartes Legion v2 embarqués dans card.legionStats (JSON),
// générés par build_legion_v2.py depuis le bundle LegionHQ V2 (2.6).
private data class LegionWeapon(val name: String, val rangeMin: Int, val rangeMax: Int,
                                val red: Int, val black: Int, val white: Int)
private data class LegionStats(
    val health: Int = 1,
    val courage: Int = 1,
    val speed: Int = 1,
    val defenseDie: String = "w",        // 'r' | 'w'
    val surgeAttack: String = "",        // 'h' crit | 'a' hit | 'b' block | '' none
    val surgeDefense: String = "",
    val miniCount: Int = 1,
    val keywords: List<String> = emptyList(),
    val weapons: List<LegionWeapon> = emptyList()
)
private object LegionStatsParser {
    fun parse(legionStats: String?): LegionStats? {
        if (legionStats.isNullOrBlank()) return null
        return try {
            val o = org.json.JSONObject(legionStats)
            val wp = buildList {
                val wa = o.optJSONArray("weapons")
                if (wa != null) for (i in 0 until wa.length()) {
                    val w = wa.getJSONObject(i)
                    val rng = w.optJSONObject("range")
                    val dice = w.optJSONObject("dice")
                    add(LegionWeapon(
                        w.optString("name", "Arme"),
                        rng?.optInt("min", 0) ?: 0, rng?.optInt("max", 0) ?: 0,
                        dice?.optInt("red", 0) ?: 0, dice?.optInt("black", 0) ?: 0, dice?.optInt("white", 0) ?: 0
                    ))
                }
            }
            val kw = buildList {
                val ka = o.optJSONArray("keywords")
                if (ka != null) for (i in 0 until ka.length()) add(ka.getString(i))
            }
            LegionStats(
                health = o.optInt("health", 1).coerceAtLeast(1),
                courage = o.optInt("courage", 1).coerceAtLeast(1),
                speed = o.optInt("speed", 1).coerceAtLeast(1),
                defenseDie = o.optString("defenseDie", "w"),
                surgeAttack = o.optString("surgeAttack", ""),
                surgeDefense = o.optString("surgeDefense", ""),
                miniCount = o.optInt("miniCount", 1).coerceAtLeast(1),
                keywords = kw,
                weapons = wp
            )
        } catch (_: Exception) { null }
    }
}

@Composable
private fun LegionUnitPage(unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>) {
    val totalPts = unit.card.points + children.sumOf { it.card.points * it.quantity }
    val stats = remember(unit.instanceId) { LegionStatsParser.parse(unit.card.legionStats) }
    // Points de vie réels de l'unité = santé par figurine × nombre de figurines.
    // Tracker de VALEUR RESTANTE : démarre plein, descend sous les dégâts (comme la coque Armada).
    val maxHp = (stats?.health?.takeIf { it > 0 } ?: 1) * (stats?.miniCount?.takeIf { it > 0 } ?: 1)
    var wounds by remember(unit.instanceId) { mutableIntStateOf(maxHp) }
    var tokens by remember(unit.instanceId) { mutableStateOf(listOf<String>()) }
    var activeCrits by remember(unit.instanceId) { mutableStateOf(listOf<CritCard>()) }
    var usedUpgrades by remember(unit.instanceId) { mutableStateOf(setOf<String>()) }
    val defColor = if ((stats?.defenseDie ?: "w") == "r") Color(0xFFFF6B6B) else Color.White

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardBlock(unit, children, totalPts)
        // ── stats réelles de la carte ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PROFIL", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    StatChip("SANTE", "${stats?.miniCount ?: 1} × ${stats?.health ?: 1}", Color(0xFFFF6B6B))
                    StatChip("COURAGE", "${stats?.courage ?: 1}", Color(0xFFFFC857))
                    StatChip("VITESSE", "${stats?.speed ?: 1}", Color(0xFF4FC3F7))
                    StatChip("DEFENSE", (stats?.defenseDie ?: "w").uppercase(), defColor)
                }
                if (!stats?.surgeAttack.isNullOrBlank() || !stats?.surgeDefense.isNullOrBlank()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Surge attaque: ${surgeLabel(stats?.surgeAttack)}", color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                        Text("Surge défense: ${surgeLabel(stats?.surgeDefense)}", color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (stats?.keywords?.isNotEmpty() == true) {
                    Text("Mots-clés: ${stats.keywords.joinToString(", ")}", color = Color(0xFF9EACBC), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        // ── suivi des blessures (valeur restante) ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("SUIVI", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { BigCounter("FIGURINES", wounds, maxHp, Color(0xFFFF6B6B), { if (wounds < maxHp) wounds++ }, { if (wounds > 0) wounds-- }) }
                HealthBar(wounds.toFloat() / maxHp, wounds, maxHp)
                // Armes de l'unité (range + dés)
                if (stats?.weapons?.isNotEmpty() == true) {
                    HorizontalDivider(color = Color(0xFF2A3A4A))
                    Text("ARMES", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    stats.weapons.forEach { w ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(w.name, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Portée ${w.rangeMin}-${w.rangeMax}", color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(diceText(w.red, w.black, w.white), color = Color(0xFF77D9A7), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                TokenSection(tokens, { tokens = tokens + it }, { tokens = tokens - it })
            }
        }
        EffectsPanel(
            effects = buildList {
                children.forEach { c -> add(ActiveEffect(EffectType.UPGRADE, c.card.displayName(), c.card.rulesText ?: "Amelioration installee", c.instanceId).copy(used = usedUpgrades.contains(c.instanceId))) }
                activeCrits.forEach { c -> add(ActiveEffect(EffectType.CRIT, c.name, c.effect, c.name)) }
            },
            critSelector = { expanded, onDismiss, onSelect ->
                CritSelectorDropdown(legionCrits, expanded, onDismiss, onSelect)
            },
            onAddCrit = { activeCrits = activeCrits + it },
            onRemoveCrit = { activeCrits = activeCrits - it },
            allCrits = legionCrits
        )
        CardPlayImage(unit.card)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF9EACBC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
private fun surgeLabel(s: String?): String = when (s) { "h" -> "Critique"; "a" -> "Touché"; "b" -> "Blocage"; "r" -> "Contre-attaque"; else -> "—" }
private fun diceText(red: Int, black: Int, white: Int): String {
    val parts = buildList { if (red > 0) add("R$red"); if (black > 0) add("N$black"); if (white > 0) add("B$white") }
    return if (parts.isEmpty()) "—" else parts.joinToString(" ")
}

// // ═══════════════════  ARMADA SHIP  ═══════════════════════
// Stats embarqués depuis le catalogue BSData ("fleet builder") dans card.shipStats (JSON).
private data class ArmadaStats(
    val hull: Int = 0,
    val shieldFront: Int = 0,
    val shieldRear: Int = 0,
    val shieldPort: Int = 0,
    val shieldStarboard: Int = 0,
    val maxSpeed: Int = 1,
    val speed: Int = 3, // valeur fixe des squadrons
    val defenseTokens: List<String> = emptyList()
)

private object ArmadaStatsParser {
    fun parse(shipStats: String?, kind: CardKind): ArmadaStats? {
        if (shipStats.isNullOrBlank()) return null
        return try {
            val o = org.json.JSONObject(shipStats)
            val shield = o.optJSONObject("shield")
            val tokArr = o.optJSONArray("defenseTokens")
            val tokens = buildList {
                if (tokArr != null) for (i in 0 until tokArr.length()) add(tokArr.getString(i))
            }
            when (kind) {
                CardKind.ARMADA_SHIP -> ArmadaStats(
                    hull = o.optInt("hull"),
                    shieldFront = shield?.optInt("front", 0) ?: 0,
                    shieldRear = shield?.optInt("rear", 0) ?: 0,
                    shieldPort = shield?.optInt("left", 0) ?: 0,
                    shieldStarboard = shield?.optInt("right", 0) ?: 0,
                    maxSpeed = o.optInt("maxSpeed", 1).coerceAtLeast(1),
                    defenseTokens = tokens
                )
                CardKind.ARMADA_SQUADRON -> ArmadaStats(
                    hull = o.optInt("hull"),
                    speed = o.optInt("speed", 3).coerceAtLeast(1)
                )
                else -> null
            }
        } catch (_: Exception) { null }
    }
}

@Composable
private fun ArmadaShipPage(unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>) {
    val totalPts = unit.card.points + children.sumOf { it.card.points * it.quantity }
    val stats = remember(unit.instanceId) { ArmadaStatsParser.parse(unit.card.shipStats, CardKind.ARMADA_SHIP) }
    // Coque + boucliers initialisés aux valeurs de base de la carte ; speed démarre à 2.
    // La coque est un tracker de valeur restante (démarre au hull de base, descend sous les dégâts).
    val maxHp = stats?.hull?.takeIf { it > 0 } ?: 15
    var hull by remember(unit.instanceId) { mutableIntStateOf(maxHp) }
    var sF by remember(unit.instanceId) { mutableIntStateOf(stats?.shieldFront ?: 0) }
    var sR by remember(unit.instanceId) { mutableIntStateOf(stats?.shieldRear ?: 0) }
    var sP by remember(unit.instanceId) { mutableIntStateOf(stats?.shieldPort ?: 0) }
    var sS by remember(unit.instanceId) { mutableIntStateOf(stats?.shieldStarboard ?: 0) }
    val maxShield = 9
    val maxSpeed = stats?.maxSpeed ?: 3
    var speed by remember(unit.instanceId) { mutableIntStateOf(2) }
    var tokens by remember(unit.instanceId) { mutableStateOf(listOf<String>()) }
    var activeCrits by remember(unit.instanceId) { mutableStateOf(listOf<CritCard>()) }
    var usedUpgrades by remember(unit.instanceId) { mutableStateOf(setOf<String>()) }
    val defTokenNames = remember(unit.instanceId) {
        val fromStats = stats?.defenseTokens.orEmpty()
        if (fromStats.isNotEmpty()) fromStats
        else ArmadaDefenseToken.entries.take(4).map { it.name }
    }
    val defTokenStates = defTokenNames.mapNotNull { name ->
        val def = ArmadaDefenseToken.entries.firstOrNull { it.name == name.uppercase() }
        if (def == null) null else def to name
    }
    // Chaque jeton (même en doublon) est une instance indépendante, clé = "name_i".
    var defTokens by remember(unit.instanceId) { mutableStateOf<Map<String, Boolean>>(defTokenStates.mapIndexed { i, (def, _) -> "${def.name}_$i" to false }.toMap()) }
    val commander = allEntries.firstOrNull { it.card.kind == CardKind.COMMANDER || (it.card.kind == CardKind.ARMADA_UPGRADE && ArmadaSlot.COMMANDER in it.card.upgradeSlots) }

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ── ship card ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) { Text(unit.card.displayName(), color = Color.White, style = MaterialTheme.typography.headlineSmall); Text("Vaisseau  •  ${unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelLarge) }
                    Text("${unit.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
                }
                if (!unit.card.rulesText.isNullOrBlank()) Text(unit.card.rulesText, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 6)
                if (commander != null) {
                    val used = usedUpgrades.contains("cmd")
                    HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 2.dp))
                    Row(Modifier.fillMaxWidth().clickable { usedUpgrades = if (used) usedUpgrades - "cmd" else usedUpgrades + "cmd" }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.weight(1f)) {
                            Text(if (used) "\u25CB " else "\u25C9 ", color = if (used) Color(0xFF5A6A7A) else Color(0xFFFFC857))
                            Text(commander.card.displayName(), color = if (used) Color(0xFF5A6A7A) else Color(0xFFFFC857), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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
                                Text(c.card.displayName(), color = if (used) Color(0xFF5A6A7A) else Color.White, style = MaterialTheme.typography.bodyMedium)
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
                    defTokenStates.forEachIndexed { i, (def, _) ->
                        val key = "${def.name}_$i"
                        val used = defTokens[key] ?: false
                        Surface(onClick = { defTokens = defTokens + (key to !used) }, shape = RoundedCornerShape(14.dp), color = if (used) Color(0xFF5A2020) else Color(0xFF1A4A2A), modifier = Modifier.weight(1f)) {
                            Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(def.icon, fontSize = 20.sp)
                                Text(def.label, color = if (used) Color(0xFFFF6B6B) else Color(0xFF77D9A7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                    MiniShield("AV", sF, { if (sF < maxShield) sF++ }, { if (sF > 0) sF-- })
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) { MiniShield("BAB", sP, { if (sP < maxShield) sP++ }, { if (sP > 0) sP-- }); MiniShield("TRIB", sS, { if (sS < maxShield) sS++ }, { if (sS > 0) sS-- }) }
                    MiniShield("ARR", sR, { if (sR < maxShield) sR++ }, { if (sR > 0) sR-- })
                }
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    BigCounter("VITESSE", speed, maxSpeed, Color(0xFF77D9A7), { if (speed < maxSpeed) speed++ }, { if (speed > 1) speed-- })
                    Spacer(Modifier.width(24.dp))
                    BigCounter("COQUE", hull, maxHp, Color(0xFFFF6B6B), { if (hull < maxHp) hull++ }, { if (hull > 0) hull-- })
                }
                if (hull < maxHp) { HealthBar(hull.toFloat() / maxHp, hull, maxHp) }
            }
        }
        // ── tokens ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp)) { TokenSection(tokens, { tokens = tokens + it }, { tokens = tokens - it }) }
        }
        // ── effects panel ──
        EffectsPanel(
            effects = buildList {
                if (commander != null) add(ActiveEffect(EffectType.COMMANDER, commander.card.displayName(), commander.card.rulesText ?: "Commandant de la flotte", "cmd_${commander.instanceId}")
                    .copy(used = usedUpgrades.contains("cmd")))
                children.forEach { c -> add(ActiveEffect(EffectType.UPGRADE, c.card.displayName(), c.card.rulesText ?: "Amelioration installee", c.instanceId)
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
        CardPlayImage(unit.card)
        Spacer(Modifier.height(20.dp))
    }
}
// ═══════════════════  COMMANDER (pas de degats critiques, equipe sur le flagship)  ═══════════════════
@Composable
private fun CommanderPage(unit: ListEntry) {
    var tokens by remember(unit.instanceId) { mutableStateOf(listOf<String>()) }
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardBlock(unit, emptyList(), unit.card.points)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("COMMANDEMENT DE LA FLOTTE", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TokenSection(tokens, { tokens = tokens + it }, { tokens = tokens - it })
            }
        }
        CardPlayImage(unit.card)
        Spacer(Modifier.height(20.dp))
    }
}

// ═══════════════════  SQUADRON  ═══════════════════════════
@Composable
private fun ArmadaSquadronPage(unit: ListEntry) {
    val stats = remember(unit.instanceId) { ArmadaStatsParser.parse(unit.card.shipStats, CardKind.ARMADA_SQUADRON) }
    val maxHp = stats?.hull?.takeIf { it > 0 } ?: 8
    var hull by remember(unit.instanceId) { mutableIntStateOf(maxHp) }; var tokens by remember(unit.instanceId) { mutableStateOf(listOf<String>()) }
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardBlock(unit, emptyList(), unit.card.points)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SUIVI ESCADRON", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { BigCounter("COQUE", hull, maxHp, Color(0xFFFF6B6B), { if (hull < maxHp) hull++ }, { if (hull > 0) hull-- }) }
                if (hull < maxHp) HealthBar(hull.toFloat() / maxHp, hull, maxHp)
                TokenSection(tokens, { tokens = tokens + it }, { tokens = tokens - it })
            }
        }
        CardPlayImage(unit.card)
        Spacer(Modifier.height(20.dp))
    }
}

// ═══════════════════  SHARED COMPONENTS  ═══════════════════

// Affiche la carte en image (la vraie carte) en bas des pages de mode partie,
// si une image est disponible pour cette carte (sinon rien).
@Composable
private fun CardPlayImage(card: CardDefinition) {
    val source: Any? = card.imageAssetPath?.let { "file:///android_asset/$it" } ?: card.imageUrl
    if (source != null) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CARTE", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                AsyncImage(
                    model = source,
                    contentDescription = "Carte de ${card.displayName()}",
                    modifier = Modifier.fillMaxWidth(0.96f).heightIn(max = 520.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}


@Composable
private fun CardBlock(unit: ListEntry, children: List<ListEntry>, totalPts: Int) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(unit.card.displayName(), color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    val kind = unit.card.legionRank?.name?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: unit.card.kind.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                    Text("$kind  •  ${unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelLarge)
                }
                Text("${unit.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleLarge)
            }
            if (!unit.card.rulesText.isNullOrBlank()) Text(unit.card.rulesText, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 6)
            if (children.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 4.dp))
                children.forEach { c -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Row(modifier = Modifier.weight(1f)) { Text("+ ", color = Color(0xFF77D9A7), fontWeight = FontWeight.Bold); Text(c.card.displayName(), color = Color.White, style = MaterialTheme.typography.bodyMedium) }; Text("${c.card.points}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium) } }
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
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(38.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) } }
            Text("$value", color = color, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(54.dp))
            Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(38.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) } }
        }
    }
}