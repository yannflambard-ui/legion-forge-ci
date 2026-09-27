package com.legionforge.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.legionforge.app.R
import com.legionforge.app.data.model.*
import com.legionforge.app.data.model.WikiSectionEntity
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

// Retrouve la section wiki correspondant à un mot-clé/titre (insensible à la casse).
private fun findWikiSection(wikiSections: List<WikiSectionEntity>, keyword: String): WikiSectionEntity? {
    val k = keyword.lowercase()
    return wikiSections.firstOrNull { s ->
        s.title.lowercase() == k || s.keywordList().any { it.lowercase() == k }
    }
}

// Retire le préfixe "[Nom]\n" du rulesText (le nom est déjà affiché dans le label de l'effet).
private fun stripBracketName(desc: String): String {
    val t = desc.trimStart()
    return if (t.startsWith("[")) {
        val end = t.indexOf(']')
        if (end > 0) t.substring(end + 1).trimStart() else t
    } else t
}

// Icône officielle du slot d'upgrade (assets/icons/upg_*.webp), teintée par la couleur du slot.
private fun slotIconPath(slot: ArmadaSlot): String? = when (slot) {
    ArmadaSlot.COMMANDER -> "icons/upg_commander.webp"
    ArmadaSlot.OFFICER -> "icons/upg_officer.webp"
    ArmadaSlot.TITLE -> "icons/upg_title.webp"
    ArmadaSlot.DEFENSIVE_RETROFIT -> "icons/upg_defensive.webp"
    ArmadaSlot.OFFENSIVE_RETROFIT -> "icons/upg_offensive.webp"
    ArmadaSlot.WEAPONS_TEAM -> "icons/upg_weapons.webp"
    ArmadaSlot.SUPPORT_TEAM -> "icons/upg_support.webp"
    ArmadaSlot.ORDNANCE -> "icons/upg_ordnance.webp"
    ArmadaSlot.TURBOLASERS -> "icons/upg_turbo.webp"
    ArmadaSlot.ION_CANNONS -> "icons/upg_ion.webp"
    ArmadaSlot.EXPERIMENTAL_RETROFIT -> "icons/upg_experimental.webp"
    ArmadaSlot.FLEET_COMMAND -> "icons/upg_fleetcmd.webp"
    ArmadaSlot.FLEET_SUPPORT -> "icons/upg_fleetsup.webp"
    else -> null
}

// Badge d'upgrade : lettre + couleur distincte par type de slot (au lieu du bleu unique).
private fun slotBadge(slot: ArmadaSlot): Pair<String, Color> = when (slot) {
    ArmadaSlot.COMMANDER -> "C" to Color(0xFFFFC857)
    ArmadaSlot.OFFICER -> "O" to Color(0xFFFFB74D)
    ArmadaSlot.TITLE -> "T" to Color(0xFFCE93D8)
    ArmadaSlot.DEFENSIVE_RETROFIT -> "DR" to Color(0xFF4DD0E1)
    ArmadaSlot.OFFENSIVE_RETROFIT -> "OR" to Color(0xFFFF8A65)
    ArmadaSlot.WEAPONS_TEAM -> "W" to Color(0xFFF06292)
    ArmadaSlot.SUPPORT_TEAM -> "S" to Color(0xFFAED581)
    ArmadaSlot.ORDNANCE -> "ORD" to Color(0xFFFFD54F)
    ArmadaSlot.TURBOLASERS -> "TB" to Color(0xFF64B5F6)
    ArmadaSlot.ION_CANNONS -> "ION" to Color(0xFF90CAF9)
    ArmadaSlot.EXPERIMENTAL_RETROFIT -> "ER" to Color(0xFFBA68C8)
    ArmadaSlot.SUPERWEAPON -> "SW" to Color(0xFFEF5350)
    else -> slot.name.first().toString() to Color(0xFF4FC3F7)
}

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
    CONTAIN("Contain", "\u26D4"),
    SALVO("Salvo", "\u21BA")
}

// ── command orders (roue de commandement) for Armada ────
enum class ArmadaCommandOrder(val label: String, val iconFile: String, val orderFile: String) {
    NAVIGATE("Navigation", "cmd_navigate.webp", "order_navigate.webp"),
    CONCENTRATE("Concentrate Fire", "cmd_concentrate.webp", "order_concentrate.webp"),
    SQUADRON("Squadron", "cmd_squadron.webp", "order_squadron.webp"),
    REPAIR("Repair", "cmd_repair.webp", "order_repair.webp")
}

// ── active effect model ─────────────────────────────────
private enum class EffectType { COMMANDER, UPGRADE, CRIT, ABILITY }
private data class ActiveEffect(val type: EffectType, val label: String, val desc: String, val id: String, val used: Boolean = false, val slot: ArmadaSlot? = null, val pts: Int = 0)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardDetailScreen(entries: List<ListEntry>, initialIndex: Int = 0, onBack: () -> Unit, vm: ArmyBuilderViewModel = viewModel()) {
    val playable = entries.filter { e ->
        e.parentInstanceId == null && (e.card.kind == CardKind.LEGION_UNIT || e.card.kind == CardKind.ARMADA_SHIP || e.card.kind == CardKind.ARMADA_SQUADRON || e.card.kind == CardKind.COMMANDER)
    }
    val safeIndex = initialIndex.coerceIn(0, (playable.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(pageCount = { playable.size.coerceAtLeast(1) }, initialPage = safeIndex)
    var round by remember { mutableIntStateOf(1) }
    // Wiki des règles : chargé pour rendre les mots-clés du texte de règles cliquables.
    val wikiSections by vm.wikiSections.collectAsState()
    var ruleSection by remember { mutableStateOf<WikiSectionEntity?>(null) }
    val onRuleClick: (WikiSectionEntity) -> Unit = { ruleSection = it }
    LaunchedEffect(Unit) { vm.loadAllWiki() }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (playable.isNotEmpty()) playable[pagerState.currentPage].card.displayName() else stringResource(R.string.play_mode), style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
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
        if (playable.isEmpty()) { Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { Text(stringResource(R.string.add_units_play), color = Color.Gray) }; return@Scaffold }
        Box(Modifier.fillMaxSize().padding(pad)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val unit = playable[page]; val children = entries.filter { it.parentInstanceId == unit.instanceId }
                // Liens wiki : ne montrer que les mots-clés du jeu courant (pas de mélange Legion/Armada)
                val unitWikiSections = remember(unit, wikiSections) {
                    wikiSections.filter { it.gameSystem == unit.card.gameSystem.name }
                }
                when (unit.card.kind) {
                    CardKind.ARMADA_SHIP -> ArmadaShipPage(unit, children, entries, unitWikiSections, onRuleClick)
                    CardKind.ARMADA_SQUADRON -> ArmadaSquadronPage(unit, unitWikiSections, onRuleClick)
                    // Degats critiques reserves aux vaisseaux capitaux (par Regle Armada). Un commandant est equipe sur un vaisseau, il n'a pas de page de degats propres.
                    CardKind.COMMANDER -> CommanderPage(unit, unitWikiSections, onRuleClick)
                    else -> LegionUnitPage(unit, children, entries, unitWikiSections, onRuleClick)
                }
            }
            if (playable.size > 1) {
                Row(Modifier.fillMaxWidth().padding(bottom = 50.dp).align(Alignment.BottomCenter), horizontalArrangement = Arrangement.Center) {
                    repeat(playable.size) { i -> Box(Modifier.padding(3.dp).size(if (i == pagerState.currentPage) 10.dp else 7.dp).clip(CircleShape).background(if (i == pagerState.currentPage) Color(0xFFFFC857) else Color(0xFF3A4A5A))) }
                }
            }
        }
    }
    // Popup de règle : clic sur un mot-clé du texte de règles -> point de règle officiel.
    ruleSection?.let { section ->
        RulePopup(section = section, onClose = { ruleSection = null })
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
private fun LegionUnitPage(unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}) {
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
        CardBlock(unit, children, totalPts, wikiSections, onRuleClick)
        // ── stats réelles de la carte ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.profile), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    StatChip(stringResource(R.string.stat_health), "${stats?.miniCount ?: 1} × ${stats?.health ?: 1}", Color(0xFFFF6B6B))
                    StatChip(stringResource(R.string.stat_courage), "${stats?.courage ?: 1}", Color(0xFFFFC857))
                    StatChip(stringResource(R.string.stat_speed), "${stats?.speed ?: 1}", Color(0xFF4FC3F7))
                    StatChip(stringResource(R.string.stat_defense), (stats?.defenseDie ?: "w").uppercase(), defColor)
                }
                if (!stats?.surgeAttack.isNullOrBlank() || !stats?.surgeDefense.isNullOrBlank()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.surge_attack, surgeLabel(stats?.surgeAttack)), color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.surge_defense, surgeLabel(stats?.surgeDefense)), color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (stats?.keywords?.isNotEmpty() == true) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        stats.keywords.forEach { kw ->
                            val section = findWikiSection(wikiSections, kw)
                            Surface(
                                onClick = { if (section != null) onRuleClick(section) },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2A3A4A)
                            ) {
                                Text(com.legionforge.app.data.i18n.I18n.keyword(kw), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = if (section != null) Color(0xFFFFC857) else Color(0xFF77D9A7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        // ── suivi des blessures (valeur restante) ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.tracking), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { BigCounter(stringResource(R.string.minis), wounds, maxHp, Color(0xFFFF6B6B), { if (wounds < maxHp) wounds++ }, { if (wounds > 0) wounds-- }) }
                HealthBar(wounds.toFloat() / maxHp, wounds, maxHp)
                // Armes de l'unité (range + dés)
                if (stats?.weapons?.isNotEmpty() == true) {
                    HorizontalDivider(color = Color(0xFF2A3A4A))
                    Text(stringResource(R.string.weapons), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    stats.weapons.forEach { w ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(w.name, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.range_label, w.rangeMin, w.rangeMax), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall)
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
                children.forEach { c -> add(ActiveEffect(EffectType.UPGRADE, c.card.displayName(), stripBracketName(c.card.rulesText ?: stringResource(R.string.upgrade_installed)), c.instanceId, slot = c.card.upgradeSlots.firstOrNull(), pts = c.card.points).copy(used = usedUpgrades.contains(c.instanceId))) }
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
@Composable
private fun surgeLabel(s: String?): String = when (s) { "h" -> stringResource(R.string.surge_crit); "a" -> stringResource(R.string.surge_hit); "b" -> stringResource(R.string.surge_block); "r" -> stringResource(R.string.surge_counter); else -> "—" }
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
    val command: Int = 1, // niveau de commande = stock max de pions d'ordre
    val squadron: Int = 0,
    val engineering: Int = 0,
    val attackFront: List<Int> = emptyList(), // [bleu, rouge, noir]
    val attackRear: List<Int> = emptyList(),
    val attackPort: List<Int> = emptyList(),
    val attackStarboard: List<Int> = emptyList(),
    val antiSquadron: List<Int> = emptyList(), // [bleu, rouge, noir]
    val battery: List<Int> = emptyList(),      // [bleu, rouge, noir]
    val keywords: List<String> = emptyList(),
    val defenseTokens: List<String> = emptyList(),
    val speedChart: List<Map<String, Int>> = emptyList(), // liste de positions, chaque position = {vitesse: nb de clics}
    val size: String = "small" // huge = 2 cadrans de bouclier par côté (Executor, Starhawk)
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
                CardKind.ARMADA_SHIP -> {
                    val attack = o.optJSONObject("attack")
                    fun arc(name: String): List<Int> {
                        val a = attack?.optJSONArray(name) ?: return emptyList()
                        return buildList { for (i in 0 until a.length()) add(a.optInt(i)) }
                    }
                    val scArr = o.optJSONArray("speedChart")
                    val speedChart = buildList {
                        if (scArr != null) for (i in 0 until scArr.length()) {
                            val pos = scArr.getJSONObject(i)
                            val vals = pos.optJSONObject("values")
                            val m = buildMap {
                                if (vals != null) {
                                    val it = vals.keys()
                                    while (it.hasNext()) { val k = it.next(); put(k, vals.optInt(k)) }
                                }
                            }
                            add(m)
                        }
                    }
                    ArmadaStats(
                        hull = o.optInt("hull"),
                        shieldFront = shield?.optInt("front", 0) ?: 0,
                        shieldRear = shield?.optInt("rear", 0) ?: 0,
                        shieldPort = shield?.optInt("left", 0) ?: 0,
                        shieldStarboard = shield?.optInt("right", 0) ?: 0,
                        maxSpeed = o.optInt("maxSpeed", 1).coerceAtLeast(1),
                        command = o.optInt("command", 1).coerceAtLeast(1),
                        squadron = o.optInt("squadron", 0),
                        engineering = o.optInt("engineering", 0),
                        attackFront = arc("front"),
                        attackRear = arc("rear"),
                        attackPort = arc("left"),
                        attackStarboard = arc("right"),
                        defenseTokens = tokens,
                        speedChart = speedChart,
                        size = o.optString("size", "small")
                    )
                }
                CardKind.ARMADA_SQUADRON -> {
                    val kwArr = o.optJSONArray("keywords")
                    val keywords = buildList { if (kwArr != null) for (i in 0 until kwArr.length()) add(kwArr.getString(i)) }
                    fun diceArr(name: String): List<Int> {
                        val a = o.optJSONArray(name) ?: return emptyList()
                        return buildList { for (i in 0 until a.length()) add(a.optInt(i)) }
                    }
                    ArmadaStats(
                        hull = o.optInt("hull"),
                        speed = o.optInt("speed", 3).coerceAtLeast(1),
                        antiSquadron = diceArr("antiSquadron"),
                        battery = diceArr("battery"),
                        defenseTokens = tokens,
                        keywords = keywords
                    )
                }
                else -> null
            }
        } catch (_: Exception) { null }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArmadaShipPage(unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}) {
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
    // Roue de commandement : ordre courant du vaisseau (null = non défini).
    var commandOrder by remember(unit.instanceId) { mutableStateOf<ArmadaCommandOrder?>(null) }
    // Pions d'ordre en stock : chaque commande a un compteur (0..N), total max = niveau de commande.
    val maxOrderStock = stats?.command ?: 1
    var orderTokens by remember(unit.instanceId) { mutableStateOf<Map<String, Int>>(ArmadaCommandOrder.entries.associate { it.name to 0 }) }
    val commander = allEntries.firstOrNull { it.card.kind == CardKind.COMMANDER || (it.card.kind == CardKind.ARMADA_UPGRADE && ArmadaSlot.COMMANDER in it.card.upgradeSlots) }

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ── ship card ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(unit.card.displayName(), color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${stringResource(R.string.kind_ship)}  •  ${unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium) }
                    Text("${unit.card.points} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium)
                }
                if (stats != null) ShipStatsBlock(stats)
                else if (!unit.card.rulesText.isNullOrBlank()) ClickableRulesText(unit.card.rulesText, wikiSections, onRuleClick, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 6)
            }
        }
        // ── defense tokens ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.defense_tokens), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    defTokenStates.forEachIndexed { i, (def, _) ->
                        val key = "${def.name}_$i"
                        val used = defTokens[key] ?: false
                        DefenseTokenDisc(def, used, onClick = { defTokens = defTokens + (key to !used) })
                    }
                }
            }
        }
        // ── command dial (roue de commandement) + pions d'ordre ──
        CommandDialCard(
            selected = commandOrder,
            onSelect = { commandOrder = if (commandOrder == it) null else it },
            orderTokens = orderTokens,
            maxStock = maxOrderStock,
            onIncOrder = { name ->
                if (orderTokens.values.sum() < maxOrderStock) orderTokens = orderTokens + (name to (orderTokens[name] ?: 0) + 1)
            },
            onDecOrder = { name -> orderTokens = orderTokens + (name to ((orderTokens[name] ?: 0) - 1).coerceAtLeast(0)) },
            wikiSections = wikiSections,
            onRuleClick = onRuleClick
        )
        // ── cadrans (boucliers & coque) + dés d'attaque par côté + matrice de manoeuvres ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.shields_hull), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                // ── cadrans des 4 arcs en croix (comme sur la carte officielle) : rectangle de dés + cercle de bouclier ──
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // AVANT : rectangle de dés AU-DESSUS du cercle de bouclier.
                    ArcCadran(ArcPos.ABOVE, stringResource(R.string.shield_front), stats?.attackFront, sF, { if (sF < maxShield) sF++ }, { if (sF > 0) sF-- })
                    // Ligne centrale : BAB (gauche) | TRIB (droite).
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
                        ArcCadran(ArcPos.LEFT, stringResource(R.string.shield_port), stats?.attackPort, sP, { if (sP < maxShield) sP++ }, { if (sP > 0) sP-- })
                        ArcCadran(ArcPos.RIGHT, stringResource(R.string.shield_starboard), stats?.attackStarboard, sS, { if (sS < maxShield) sS++ }, { if (sS > 0) sS-- })
                    }
                    // ARRIERE : rectangle de dés EN DESSOUS du cercle de bouclier.
                    ArcCadran(ArcPos.BELOW, stringResource(R.string.shield_rear), stats?.attackRear, sR, { if (sR < maxShield) sR++ }, { if (sR > 0) sR-- })
                }
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    BigCounter(stringResource(R.string.stat_speed), speed, maxSpeed, Color(0xFF77D9A7), { if (speed < maxSpeed) speed++ }, { if (speed > 1) speed-- })
                    Spacer(Modifier.width(24.dp))
                    BigCounter(stringResource(R.string.hull), hull, maxHp, Color(0xFFFF6B6B), { if (hull < maxHp) hull++ }, { if (hull > 0) hull-- })
                }
                if (hull < maxHp) { HealthBar(hull.toFloat() / maxHp, hull, maxHp) }
                // Matrice de manoeuvres : triangulaire 4x4 (comme la carte officielle).
                // X = vitesse (1..maxSpeed), Y = position (nb de clics sur l'outil de manoeuvre).
                // Valeur : I = 1 clic, II = 2 clics, - = 0. Cases vides (haut-droite) = bordeaux.
                if (stats?.speedChart?.isNotEmpty() == true) {
                    HorizontalDivider(color = Color(0xFF2A3A4A))
                    Text(stringResource(R.string.maneuver_matrix), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    val maxPos = stats.speedChart.size.coerceAtLeast(1)
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // En-tête : vitesses (X).
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Spacer(Modifier.width(28.dp)) // coin vide (label position)
                            (1..maxSpeed).forEach { v ->
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Text("V$v", color = Color(0xFF9EACBC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        // Lignes : positions (Y).
                        stats.speedChart.forEachIndexed { posIdx, posMap ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                                    Text("${posIdx + 1}", color = Color(0xFF9EACBC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                (1..maxSpeed).forEach { v ->
                                    val n = posMap[v.toString()] ?: -1 // -1 = case vide (hors triangle)
                                    val cellColor = if (n < 0) Color(0xFF800000) else Color.White
                                    val cellText = when { n < 0 -> ""; n == 0 -> "-"; n == 1 -> "I"; n == 2 -> "II"; else -> "III" }
                                    Surface(shape = RoundedCornerShape(6.dp), color = cellColor, modifier = Modifier.weight(1f).height(30.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(cellText, color = if (n < 0) Color(0xFF800000) else Color(0xFF1B2B4B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // ── effects panel ──
        EffectsPanel(
            effects = buildList {
                if (commander != null) add(ActiveEffect(EffectType.COMMANDER, commander.card.displayName(), stripBracketName(commander.card.rulesText ?: stringResource(R.string.fleet_commander)), "cmd_${commander.instanceId}", pts = commander.card.points)
                    .copy(used = usedUpgrades.contains("cmd")))
                children.forEach { c ->
                    // Le commandant est déjà affiché dans sa propre section (EffectType.COMMANDER) : pas dans la liste des upgrades.
                    if (c.card.kind == CardKind.COMMANDER || (c.card.kind == CardKind.ARMADA_UPGRADE && ArmadaSlot.COMMANDER in c.card.upgradeSlots)) return@forEach
                    add(ActiveEffect(EffectType.UPGRADE, c.card.displayName(), stripBracketName(c.card.rulesText ?: stringResource(R.string.upgrade_installed)), c.instanceId, slot = c.card.upgradeSlots.firstOrNull(), pts = c.card.points)
                        .copy(used = usedUpgrades.contains(c.instanceId)))
                }
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
private fun CommanderPage(unit: ListEntry, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}) {
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardBlock(unit, emptyList(), unit.card.points, wikiSections, onRuleClick)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.fleet_command), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
        CardPlayImage(unit.card)
        Spacer(Modifier.height(20.dp))
    }
}

// ═══════════════════  SQUADRON  ═══════════════════════════
@Composable
private fun ArmadaSquadronPage(unit: ListEntry, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}) {
    val stats = remember(unit.instanceId) { ArmadaStatsParser.parse(unit.card.shipStats, CardKind.ARMADA_SQUADRON) }
    val maxHp = stats?.hull?.takeIf { it > 0 } ?: 8
    var hull by remember(unit.instanceId) { mutableIntStateOf(maxHp) }
    var activated by remember(unit.instanceId) { mutableStateOf(false) }
    // Jetons de défense des escadrons uniques (ex: "2 Brace", "Brace, Scatter").
    val defTokenNames = remember(unit.instanceId) { stats?.defenseTokens.orEmpty() }
    val defTokenStates = defTokenNames.mapNotNull { name ->
        val def = ArmadaDefenseToken.entries.firstOrNull { it.name == name.uppercase() }
        if (def == null) null else def to name
    }
    var defTokens by remember(unit.instanceId) { mutableStateOf<Map<String, Boolean>>(defTokenStates.mapIndexed { i, (def, _) -> "${def.name}_$i" to false }.toMap()) }
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardBlock(unit, emptyList(), unit.card.points, wikiSections, onRuleClick)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.squadron_tracking), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    BigCounter(stringResource(R.string.hull), hull, maxHp, Color(0xFFFF6B6B), { if (hull < maxHp) hull++ }, { if (hull > 0) hull-- })
                    Spacer(Modifier.width(20.dp))
                    // Coût de l'escadron affiché à côté de la coque.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.cost), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("${unit.card.points}", color = Color(0xFFFFC857), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (hull < maxHp) HealthBar(hull.toFloat() / maxHp, hull, maxHp)
                if (stats?.keywords?.isNotEmpty() == true) {
                    HorizontalDivider(color = Color(0xFF2A3A4A))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        stats.keywords.forEach { kw ->
                            val section = findWikiSection(wikiSections, kw)
                            Surface(
                                onClick = { if (section != null) onRuleClick(section) },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2A3A4A)
                            ) {
                                Text(kw, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = if (section != null) Color(0xFFFFC857) else Color(0xFF77D9A7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        if (defTokenStates.isNotEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.defense_tokens), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        defTokenStates.forEachIndexed { i, (def, _) ->
                            val key = "${def.name}_$i"
                            val used = defTokens[key] ?: false
                            DefenseTokenDisc(def, used, onClick = { defTokens = defTokens + (key to !used) })
                        }
                    }
                }
            }
        }
        // ── activation token ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SquadronActivationToken(
                    activated = activated,
                    onToggle = { activated = !activated },
                    onWikiClick = { findWikiSection(wikiSections, "activation")?.let(onRuleClick) }
                )
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
                Text(stringResource(R.string.card_label), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                AsyncImage(
                    model = source,
                    contentDescription = stringResource(R.string.icon_desc, card.displayName()),
                    modifier = Modifier.fillMaxWidth(0.96f).heightIn(max = 520.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}


@Composable
private fun CardBlock(unit: ListEntry, children: List<ListEntry>, totalPts: Int, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}) {
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
            if (!unit.card.rulesText.isNullOrBlank()) ClickableRulesText(unit.card.rulesText, wikiSections, onRuleClick, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 6)
            if (children.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFF2A3A4A), modifier = Modifier.padding(vertical = 4.dp))
                children.forEach { c -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Row(modifier = Modifier.weight(1f)) { Text("+ ", color = Color(0xFF77D9A7), fontWeight = FontWeight.Bold); Text(c.card.displayName(), color = Color.White, style = MaterialTheme.typography.bodyMedium) }; Text("${c.card.points}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium) } }
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text(stringResource(R.string.total_pts, totalPts), color = Color(0xFFFFC857), fontWeight = FontWeight.Bold) }
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
            Text(stringResource(R.string.active_effects), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Surface(onClick = { expandedCrit = true }, shape = RoundedCornerShape(10.dp), color = Color(0xFF5A2020)) {
                Text(stringResource(R.string.add_crit), Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color(0xFFFF6B6B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        // Crit selector dropdown
        critSelector(expandedCrit, { expandedCrit = false }, { c -> onAddCrit(c); expandedCrit = false })

        // Effect list
        if (effects.isEmpty()) {
            Text(stringResource(R.string.no_effects), color = Color(0xFF5A6A7A), style = MaterialTheme.typography.bodySmall)
        } else {
            effects.forEach { eff ->
                val (icon, iconColor) = when {
                    eff.type == EffectType.UPGRADE -> if (eff.slot != null) slotBadge(eff.slot) else ("U" to Color(0xFF4FC3F7))
                    eff.type == EffectType.COMMANDER -> "C" to Color(0xFFFFC857)
                    eff.type == EffectType.CRIT -> "\u26A1" to Color(0xFFFF6B6B)
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
                                Box(Modifier.size(24.dp).clip(CircleShape).background(if (eff.used) Color(0xFF3A3A4A) else Color(0xFF1E2A3A)), contentAlignment = Alignment.Center) {
                                    val iconPath = when {
                                        eff.type == EffectType.UPGRADE && eff.slot != null -> slotIconPath(eff.slot)
                                        eff.type == EffectType.COMMANDER -> "icons/upg_commander.webp"
                                        else -> null
                                    }
                                    if (iconPath != null) {
                                        AsyncImage(
                                            model = "file:///android_asset/$iconPath",
                                            contentDescription = eff.label,
                                            modifier = Modifier.size(18.dp),
                                            colorFilter = ColorFilter.tint(if (eff.used) Color(0xFF5A6A7A) else iconColor)
                                        )
                                    } else {
                                        Text(icon, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (eff.used) Color(0xFF5A6A7A) else iconColor)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(eff.label, color = if (eff.used) Color(0xFF5A6A7A) else Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = if (eff.type == EffectType.CRIT) FontWeight.Bold else FontWeight.Normal)
                                        if (eff.used) { Spacer(Modifier.width(6.dp)); Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF3A3A4A)) { Text(stringResource(R.string.activated), Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = Color(0xFF5A6A7A), fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
                                    }
                                    Text(
                                        when (eff.type) { EffectType.COMMANDER -> stringResource(R.string.effect_commander); EffectType.UPGRADE -> stringResource(R.string.effect_upgrade); EffectType.CRIT -> stringResource(R.string.effect_crit); EffectType.ABILITY -> stringResource(R.string.effect_ability) },
                                        color = (if (eff.used) Color(0xFF5A6A7A) else iconColor).copy(alpha = 0.6f), fontSize = 10.sp
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (eff.pts > 0) Text("${eff.pts} pts", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(if (selectedEffect == eff) "\u25B2" else "\u25BC", color = Color(0xFF9EACBC), fontSize = 12.sp)
                            }
                        }
                        // Expanded description
                        if (selectedEffect == eff) {
                            Spacer(Modifier.height(8.dp))
                            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color(0xFF0A0E15)) {
                                Text(eff.desc, Modifier.padding(12.dp), color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
                            }
                            if (eff.type == EffectType.CRIT) {
                                Spacer(Modifier.height(4.dp))
                                TextButton(onClick = { onRemoveCrit(allCrits.firstOrNull { it.name == eff.label } ?: return@TextButton) }) { Text(stringResource(R.string.remove_crit), color = Color(0xFFFF6B6B), style = MaterialTheme.typography.labelSmall) }
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
private fun DefenseTokenDisc(def: ArmadaDefenseToken, used: Boolean, onClick: () -> Unit) {
    val ringColor = if (used) Color(0xFFFF6B6B) else Color(0xFF77D9A7)
    // Les jetons de defense officiels (render fandom, 100x52) : vert = PRET, rouge = UTILISE.
    val tokenFile = "tokens/${def.name.lowercase()}_${if (used) "exhausted" else "ready"}.webp"
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color(0xFF192330),
            modifier = Modifier.size(52.dp),
            border = androidx.compose.foundation.BorderStroke(3.dp, ringColor)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(3.dp)) {
                AsyncImage(
                    model = "file:///android_asset/$tokenFile",
                    contentDescription = def.label,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Text(def.label, color = ringColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(if (used) stringResource(R.string.used) else stringResource(R.string.ready), color = ringColor.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

// ── stats du vaisseau (au lieu du JSON brut) ─────────────
@Composable
private fun ShipStatsBlock(stats: ArmadaStats) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatChip("CMD", stats.command, Color(0xFFFFC857), modifier = Modifier.weight(1f))
            StatChip("SQN", stats.squadron, Color(0xFF4FC3F7), modifier = Modifier.weight(1f))
            StatChip("ENG", stats.engineering, Color(0xFF77D9A7), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatChip(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f), modifier = modifier) {
        Column(Modifier.padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(label, color = color.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ── dés d'attaque : un losange par COULEUR avec le nombre de dés dedans (comme la carte officielle) ──
// Losange plein de couleur avec le nombre de dés de cette couleur au centre.
@Composable
private fun DiceDiamond(count: Int, color: Color, size: Dp = 20.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width; val h = this.size.height
            val path = Path().apply {
                moveTo(w / 2f, 0f)          // pointe haut
                lineTo(w, h / 2f)           // pointe droite
                lineTo(w / 2f, h)           // pointe bas
                lineTo(0f, h / 2f)          // pointe gauche
                close()
            }
            drawPath(path, color = color)
        }
        Text("$count", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

// Rangée de dés d'attaque d'un arc : UN losange par couleur, avec le nombre de dés de cette couleur.
@Composable
private fun AttackDiceRow(dice: List<Int>?) {
    if (dice.isNullOrEmpty() || dice.all { it <= 0 }) return
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        val blue = dice.getOrNull(0) ?: 0
        val red = dice.getOrNull(1) ?: 0
        val black = dice.getOrNull(2) ?: 0
        if (blue > 0) DiceDiamond(blue, Color(0xFF4FC3F7))
        if (red > 0) DiceDiamond(red, Color(0xFFFF6B6B))
        if (black > 0) DiceDiamond(black, Color(0xFF3A3A4A))
    }
}

// ── cadrans des arcs (imité de la carte officielle) ──────────
// Position du rectangle de dés par rapport au cercle de bouclier.
private enum class ArcPos { ABOVE, BELOW, LEFT, RIGHT }

// Cadran d'un arc : rectangle de dés positionné autour du cercle de bouclier.
@Composable
private fun ArcCadran(pos: ArcPos, label: String, dice: List<Int>?, value: Int, onInc: () -> Unit, onDec: () -> Unit) {
    val rect: @Composable () -> Unit = { DiceRect(dice) }
    val shield: @Composable () -> Unit = { CircleShield(label, value, onInc, onDec) }
    when (pos) {
        ArcPos.ABOVE -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { rect(); shield() }
        ArcPos.BELOW -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { shield(); rect() }
        ArcPos.LEFT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { rect(); shield() }
        ArcPos.RIGHT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { shield(); rect() }
    }
}

// Rectangle blanc contenant les dés d'attaque en losanges de couleur (comme le cadran de la carte).
@Composable
private fun DiceRect(dice: List<Int>?) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFE8EDF5),   // fond blanc cassé du rectangle sur la carte
        modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 40.dp)
    ) {
        Box(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            if (dice.isNullOrEmpty() || dice.all { it <= 0 }) {
                Text("—", color = Color(0xFF9AA7B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                AttackDiceRow(dice)
            }
        }
    }
}

// Cercle blanc à fine bordure bleue contenant la valeur de bouclier (comme sur la carte officielle).
@Composable
private fun CircleShield(label: String, value: Int, onInc: () -> Unit, onDec: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(26.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(44.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4FC3F7))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$value", color = Color(0xFF1B2B4B), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(26.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
        }
        Text(label, color = Color(0xFF9EACBC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// ── roue de commandement (dial) + pions d'ordre ──────────
@Composable
private fun CommandDialCard(
    selected: ArmadaCommandOrder?,
    onSelect: (ArmadaCommandOrder) -> Unit,
    orderTokens: Map<String, Int>,
    maxStock: Int,
    onIncOrder: (String) -> Unit,
    onDecOrder: (String) -> Unit,
    wikiSections: List<WikiSectionEntity> = emptyList(),
    onRuleClick: (WikiSectionEntity) -> Unit = {}
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ROUE DE COMMANDEMENT", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CommandDialButton(order = ArmadaCommandOrder.NAVIGATE, selected = selected == ArmadaCommandOrder.NAVIGATE, onClick = { onSelect(ArmadaCommandOrder.NAVIGATE) }, modifier = Modifier.weight(1f))
                CommandDialButton(order = ArmadaCommandOrder.CONCENTRATE, selected = selected == ArmadaCommandOrder.CONCENTRATE, onClick = { onSelect(ArmadaCommandOrder.CONCENTRATE) }, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CommandDialButton(order = ArmadaCommandOrder.SQUADRON, selected = selected == ArmadaCommandOrder.SQUADRON, onClick = { onSelect(ArmadaCommandOrder.SQUADRON) }, modifier = Modifier.weight(1f))
                CommandDialButton(order = ArmadaCommandOrder.REPAIR, selected = selected == ArmadaCommandOrder.REPAIR, onClick = { onSelect(ArmadaCommandOrder.REPAIR) }, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = Color(0xFF2A3A4A))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("PIONS D'ORDRE", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("STOCK ${orderTokens.values.sum()} / $maxStock", color = Color(0xFF77D9A7), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArmadaCommandOrder.entries.forEach { order ->
                    val count = orderTokens[order.name] ?: 0
                    OrderTokenDisc(
                        order = order,
                        count = count,
                        onWikiClick = { findWikiSection(wikiSections, order.label)?.let(onRuleClick) },
                        onInc = { onIncOrder(order.name) },
                        onDec = { onDecOrder(order.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandDialButton(order: ArmadaCommandOrder, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ringColor = if (selected) Color(0xFFFFC857) else Color(0xFF2A3A4A)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (selected) Color(0xFF2A3A4A) else Color(0xFF192330),
            modifier = Modifier.size(64.dp),
            border = androidx.compose.foundation.BorderStroke(3.dp, ringColor)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                AsyncImage(
                    model = "file:///android_asset/tokens/${order.iconFile}",
                    contentDescription = order.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        Text(order.label, color = if (selected) Color(0xFFFFC857) else Color(0xFF9EACBC), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun OrderTokenDisc(order: ArmadaCommandOrder, count: Int, onWikiClick: () -> Unit, onInc: () -> Unit, onDec: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            onClick = onWikiClick,
            shape = CircleShape,
            color = Color(0xFF192330),
            modifier = Modifier.size(52.dp),
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF77D9A7))
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(3.dp)) {
                AsyncImage(
                    model = "file:///android_asset/tokens/${order.orderFile}",
                    contentDescription = order.label,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        // Compteur de stock sous le pion (empilable, max total = niveau de commande).
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(20.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
            Text("$count", color = Color(0xFF77D9A7), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(20.dp))
            Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(20.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
        }
        Text(order.label, color = Color(0xFF9EACBC), fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

// Jeton d'activation d'escadron : disque pleine-face bleu (ACTIF) / rouge (INACTIF) comme le jeton du socle de chasseur.
// Tap sur le disque = retourner (activer/désactiver). Petit "i" à côté = définition wiki.
@Composable
private fun SquadronActivationToken(activated: Boolean, onToggle: () -> Unit, onWikiClick: () -> Unit) {
    val face = if (activated) Color(0xFF2E7DD1) else Color(0xFFC0392B)   // bleu actif / rouge inactif
    val faceLight = if (activated) Color(0xFF4FC3F7) else Color(0xFFE57373)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                onClick = onToggle,
                shape = CircleShape,
                color = face,
                modifier = Modifier.size(58.dp),
                border = androidx.compose.foundation.BorderStroke(3.dp, faceLight)
            ) {
                Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                    // Fuseau de chasseur (silhouette delta) au centre du jeton.
                    val w = size.width; val h = size.height
                    val cx = w / 2f; val cy = h / 2f
                    val body = Path().apply {
                        moveTo(cx, cy - h * 0.30f)          // nez
                        lineTo(cx + w * 0.10f, cy + h * 0.18f) // arrière droit
                        lineTo(cx + w * 0.30f, cy + h * 0.30f) // aile droite
                        lineTo(cx + w * 0.16f, cy + h * 0.10f)
                        lineTo(cx + w * 0.16f, cy + h * 0.30f) // aile droite bas
                        lineTo(cx, cy + h * 0.22f)
                        lineTo(cx - w * 0.16f, cy + h * 0.30f)
                        lineTo(cx - w * 0.16f, cy + h * 0.10f)
                        lineTo(cx - w * 0.30f, cy + h * 0.30f) // aile gauche
                        lineTo(cx - w * 0.10f, cy + h * 0.18f)
                        close()
                    }
                    drawPath(body, color = Color.White)
                }
            }
            Surface(
                onClick = onWikiClick,
                shape = CircleShape,
                color = Color(0xFF2A3A4A),
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { Text("i", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Text(if (activated) "ACTIF" else "INACTIF", color = faceLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TokenSection(tokens: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.tokens_markers), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelLarge)
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
        Text(stringResource(R.string.tap_token), color = Color(0xFF5A6A7A), style = MaterialTheme.typography.labelSmall)
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
