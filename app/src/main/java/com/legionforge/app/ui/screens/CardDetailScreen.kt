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
import com.legionforge.app.ui.viewmodel.PlaySyncViewModel
import com.legionforge.app.data.nearby.ShareMode
import com.legionforge.app.util.rememberNearbyPermissionAction
import com.legionforge.app.util.NearbyDiagnosticPanel
import androidx.lifecycle.viewmodel.compose.viewModel

// Retrouve la section wiki correspondant à un mot-clé/titre (insensible à la casse).
internal fun findWikiSection(wikiSections: List<WikiSectionEntity>, keyword: String): WikiSectionEntity? {
    val k = keyword.lowercase()
    return wikiSections.firstOrNull { s ->
        s.title.lowercase() == k || s.keywordList().any { it.lowercase() == k }
    }
}

// Retire le préfixe "[Nom]\n" du rulesText (le nom est déjà affiché dans le label de l'effet).
internal fun stripBracketName(desc: String): String {
    val t = desc.trimStart()
    return if (t.startsWith("[")) {
        val end = t.indexOf(']')
        if (end > 0) t.substring(end + 1).trimStart() else t
    } else t
}

// Retire le nom de l'effet en tête de description (déjà affiché dans le label), pour ne pas le répéter
// quand l'effet est déplié. Gère "[Nom]\n..." et "Nom\n...".
internal fun stripLeadingName(desc: String, name: String): String {
    val t = desc.trimStart()
    if (t.startsWith("[")) {
        val end = t.indexOf(']')
        if (end > 0) return t.substring(end + 1).trimStart()
    }
    if (name.isNotEmpty() && t.startsWith(name)) {
        val rest = t.removePrefix(name).trimStart()
        if (rest.startsWith("\n")) return rest.trimStart()
    }
    return t
}

// Icône officielle du slot d'upgrade (assets/icons/upg_*.webp), teintée par la couleur du slot.
internal fun slotIconPath(slot: ArmadaSlot): String? = when (slot) {
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
internal fun slotBadge(slot: ArmadaSlot): Pair<String, Color> = when (slot) {
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
internal val armadaCrits = listOf(
    CritCard("Blinded Laser", "Le vaisseau ne peut pas utiliser l'armement principal ce tour."),
    CritCard("Burning Sensors", "Le vaisseau saute son activation et gagne 1 jeton d'état."),
    CritCard("Damaged Control", "Le vaisseau ne peut pas utiliser son jeton de commandement suivant."),
    CritCard("Damaged Weapons", "Les dés rouges du vaisseau sont relancés. Piochez une nouvelle carte dégâts."),
    CritCard("Engines Damaged", "Le vaisseau ne peut pas se déplacer de sa vitesse maximale."),
    CritCard("Hull Breach", "Ignorez les boucliers sur les dés de dégâts suivants."),
    CritCard("Shaken Crew", "Le vaisseau saute son activation."),
    CritCard("Structural Damage", "Tous les dégâts subis par la coque sont doublés ce tour.")
)
internal val legionCrits = listOf(
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
internal enum class EffectType { COMMANDER, UPGRADE, CRIT, ABILITY }
internal data class ActiveEffect(val type: EffectType, val label: String, val desc: String, val id: String, val used: Boolean = false, val slot: ArmadaSlot? = null, val pts: Int = 0)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardDetailScreen(entries: List<ListEntry>, initialIndex: Int = 0, onBack: () -> Unit, listId: String? = null, vm: ArmyBuilderViewModel = viewModel(), playSyncVm: PlaySyncViewModel = viewModel()) {
    // Charge la liste dans CE vm (scopé à l'écran play) : sinon currentList est null et la
    // synchro 2 joueurs ne peut pas envoyer la liste adverse.
    if (listId != null) androidx.compose.runtime.LaunchedEffect(listId) { vm.openList(listId) }
    val playable = entries.filter { e ->
        e.parentInstanceId == null && (e.card.kind == CardKind.LEGION_UNIT || e.card.kind == CardKind.ARMADA_SHIP || e.card.kind == CardKind.ARMADA_SQUADRON || e.card.kind == CardKind.COMMANDER)
    }
    val safeIndex = initialIndex.coerceIn(0, (playable.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(pageCount = { playable.size.coerceAtLeast(1) }, initialPage = safeIndex)
    var round by remember { mutableIntStateOf(1) }
    // Mode 2 joueurs : bascule entre mon armée et l'armée adverse (sync live via Nearby).
    var twoPlayer by remember { mutableStateOf(false) }
    var viewingOpponent by remember { mutableStateOf(false) }
    val syncMode by playSyncVm.mode.collectAsState()
    val syncRound by playSyncVm.receivedRound.collectAsState()
    val opponentListJson by playSyncVm.opponentListJson.collectAsState()
    // Wiki des règles : chargé pour rendre les mots-clés du texte de règles cliquables.
    val wikiSections by vm.wikiSections.collectAsState()
    var ruleSection by remember { mutableStateOf<WikiSectionEntity?>(null) }
    val onRuleClick: (WikiSectionEntity) -> Unit = { ruleSection = it }
    LaunchedEffect(Unit) { vm.loadAllWiki() }
    // Applique le round reçu de l'autre téléphone.
    LaunchedEffect(syncRound) { if (twoPlayer && syncRound != null && syncRound != round) round = syncRound!! }
    // Émet le round quand il change en mode 2 joueurs.
    LaunchedEffect(round) { if (twoPlayer && syncMode == ShareMode.CONNECTED) playSyncVm.sendRound(round) }
    // Envoie la liste au démarrage de la session 2 joueurs.
    LaunchedEffect(syncMode) {
        if (twoPlayer && syncMode == ShareMode.CONNECTED) playSyncVm.sendList(vm.currentList.value, vm.entries.value)
    }
    // Envoie la liste dès qu'elle est chargée ET qu'on est connecté (openList est async,
    // donc currentList peut être null au moment de la connexion -> on réessaie à chaque
    // changement de currentList).
    LaunchedEffect(vm.currentList.value, syncMode) {
        val l = vm.currentList.value
        if (twoPlayer && syncMode == ShareMode.CONNECTED && l != null) {
            playSyncVm.sendList(l, vm.entries.value)
        }
    }
    // Quand on ouvre l'écran ADVERSE : renvoie notre liste + demande celle de l'adversaire.
    // (Le sendList initial peut rater si la liste n'était pas chargée à la connexion.)
    LaunchedEffect(viewingOpponent, syncMode) {
        if (viewingOpponent && syncMode == ShareMode.CONNECTED) {
            playSyncVm.sendList(vm.currentList.value, vm.entries.value)
            playSyncVm.requestList()
        }
    }
    // Sync par unité : map observable des états, alimentée puis diffusée.
    val stateMap = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateMapOf<String, com.legionforge.app.data.nearby.SyncedUnitState>() }
    val receivedUnits by playSyncVm.receivedUnits.collectAsState()
    val isConnected = syncMode == ShareMode.CONNECTED
    // Gain de permission runtime avant toute action Nearby (mode 2 joueurs).
    val runWithPermission = rememberNearbyPermissionAction()
    // Cartes de commandement Legion : chargement + deck (ajout/marquage jouée).
    val legionContext = androidx.compose.ui.platform.LocalContext.current
    val cmdFaction = vm.currentList.value?.factionId ?: ""
    val isLegion = vm.currentList.value?.gameSystem == "LEGION_V2"
    val cmdCards = remember { loadCommandCards(legionContext) }
    var commandDeck by remember { mutableStateOf(setOf<String>()) }
    var playedCommands by remember { mutableStateOf(setOf<String>()) }
    var showCmdSheet by remember { mutableStateOf(false) }
    // Battle deck (Legion v2) : 9 cartes (3+3+3) + mission (carte révélée par catégorie).
    val battleCards = remember { loadBattleCards(legionContext) }
    var battleDeck by remember { mutableStateOf(setOf<String>()) }
    var battleRevealed by remember { mutableStateOf(mapOf<String, String>()) }
    var showBattleSheet by remember { mutableStateOf(false) }
    // Applique les états reçus de l'autre téléphone.
    LaunchedEffect(receivedUnits) {
        receivedUnits.forEach { st ->
            stateMap[st.instanceId] = stateMap[st.instanceId]?.let { existing ->
                existing.copy(hull = st.hull ?: existing.hull, wounds = st.wounds ?: existing.wounds).takeIf { it != existing } ?: existing
            } ?: st
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.play_mode), style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            actions = {
                            // Bouton 2 joueurs : active/désactive le mode sync live.
                            Surface(onClick = {
                                twoPlayer = !twoPlayer
                                if (twoPlayer && syncMode == ShareMode.IDLE) runWithPermission { playSyncVm.startHost() }
                                if (!twoPlayer) { viewingOpponent = false; playSyncVm.stop() }
                            }, shape = RoundedCornerShape(8.dp), color = if (twoPlayer) Color(0xFFFFC857) else Color(0xFF2A3A4A), modifier = Modifier.padding(end = 4.dp)) {
                                Box(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.play_2players), color = if (twoPlayer) Color(0xFF0A0E15) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            // Bascule Mon armée / Armée adverse (mode 2 joueurs).
                            if (twoPlayer) {
                                Surface(onClick = { viewingOpponent = !viewingOpponent }, shape = RoundedCornerShape(8.dp), color = Color(0xFF1A2330), modifier = Modifier.padding(end = 4.dp)) {
                                    Box(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text(if (viewingOpponent) stringResource(R.string.play_opponent) else stringResource(R.string.play_my_army), color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            // Bouton deck de cartes de commandement (Legion only).
                            if (isLegion) {
                                Surface(onClick = { showCmdSheet = true }, shape = RoundedCornerShape(8.dp), color = if (commandDeck.isNotEmpty()) Color(0xFF1E3A2A) else Color(0xFF2A3A4A), modifier = Modifier.padding(end = 4.dp)) {
                                    Box(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text("CMD ${commandDeck.size}", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            // Bouton battle deck (battle cards v2, Legion only).
                            if (isLegion) {
                                Surface(onClick = { showBattleSheet = true }, shape = RoundedCornerShape(8.dp), color = if (battleDeck.isNotEmpty()) Color(0xFF1E3A2A) else Color(0xFF2A3A4A), modifier = Modifier.padding(end = 4.dp)) {
                                    Box(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                                        Text("BAT ${battleDeck.size}", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
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
            // Mode 2 joueurs : panneau de connexion si pas encore connecté.
            if (twoPlayer && syncMode != ShareMode.CONNECTED) {
                val endpoints by playSyncVm.endpoints.collectAsState()
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.play_2players), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { runWithPermission { playSyncVm.startHost() } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (syncMode == ShareMode.ADVERTISING) Color(0xFFFFC857) else Color(0xFF1A2330))) {
                            Text(stringResource(R.string.play_host), color = if (syncMode == ShareMode.ADVERTISING) Color(0xFF0A0E15) else Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { runWithPermission { playSyncVm.startClient() } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (syncMode == ShareMode.DISCOVERING) Color(0xFFFFC857) else Color(0xFF1A2330))) {
                            Text(stringResource(R.string.play_client), color = if (syncMode == ShareMode.DISCOVERING) Color(0xFF0A0E15) else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (syncMode == ShareMode.DISCOVERING) {
                                            if (endpoints.isEmpty()) Text(stringResource(R.string.share_no_endpoints), color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodySmall)
                                            endpoints.forEach { ep ->
                                                Card(onClick = { runWithPermission { playSyncVm.connectTo(ep.endpointId) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
                                                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Text(ep.name, color = Color.White, fontWeight = FontWeight.Bold)
                                                        Text(stringResource(R.string.share_connect), color = Color(0xFFFFC857), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        // Diagnostic Nearby : état des permissions + erreur traduite + bouton Signaler (→ GitHub).
                                                                                val syncError by playSyncVm.error.collectAsState()
                                                                                NearbyDiagnosticPanel(
                                                                                    error = syncError,
                                                                                    onReport = { body -> com.legionforge.app.util.CrashReporter.reportEvent("Nearby 2 joueurs (${android.os.Build.MODEL})", body) }
                                                                                )
                }
                return@Box
            }
            // Mode 2 joueurs : affichage de l'armée adverse (reçue via Nearby).
            if (viewingOpponent) {
                        val opp = remember(opponentListJson) {
                            try { com.google.gson.Gson().fromJson(opponentListJson, com.legionforge.app.ui.viewmodel.ShareListPayload::class.java) } catch (_: Exception) { null }
                        }
                        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.play_opponent), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (opp == null) {
                                Text(stringResource(R.string.play_connecting), color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodyMedium)
                                // Synchro manuelle : demande à l'autre joueur de renvoyer sa liste.
                                Button(onClick = { playSyncVm.requestList() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2330))) {
                                    Text("Renvoyer la synchro", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)
                                }
                                // Rapport GitHub : aide au diagnostic quand la liste adverse n'arrive pas.
                                TextButton(onClick = {
                                    com.legionforge.app.util.CrashReporter.reportEvent(
                                        "Synchro 2 joueurs: liste adverse absente (${android.os.Build.MODEL}) ${System.currentTimeMillis()}",
                                        "Mode=${syncMode} · connecté=${playSyncVm.connectedEndpoint.value != null} · " +
                                        "liste adverse reçue=${opponentListJson != null} · " +
                                        "ma liste chargée=${vm.currentList.value != null} · " +
                                        "erreur=${playSyncVm.error.value ?: "aucune"}"
                                    )
                                }) { Text("Signaler sur GitHub", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            } else {
                                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2330))) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(opp.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                                        Text("${opp.gameSystem} • ${opp.factionId} • ${opp.pointsLimit} pts", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelMedium)
                                        Text("${opp.entries.size} unités", color = Color(0xFF8F9BAD), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Text(stringResource(R.string.play_opponent_synced), color = Color(0xFF77D9A7), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        return@Box
                    }
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val unit = playable[page]; val children = entries.filter { it.parentInstanceId == unit.instanceId }
                // Liens wiki : ne montrer que les mots-clés du jeu courant (pas de mélange Legion/Armada)
                val unitWikiSections = remember(unit, wikiSections) {
                    wikiSections.filter { it.gameSystem == unit.card.gameSystem.name }
                }
                when (unit.card.kind) {
                    CardKind.ARMADA_SHIP -> ArmadaShipPage(unit, children, entries, unitWikiSections, onRuleClick,
                        hullValue = stateMap[unit.instanceId]?.hull,
                        onHullChange = { h ->
                            stateMap[unit.instanceId] = com.legionforge.app.data.nearby.SyncedUnitState(unit.instanceId, hull = h)
                            if (isConnected) playSyncVm.sendState(listOf(stateMap[unit.instanceId]!!))
                        })
                    CardKind.ARMADA_SQUADRON -> ArmadaSquadronPage(unit, unitWikiSections, onRuleClick)
                    // Degats critiques reserves aux vaisseaux capitaux (par Regle Armada). Un commandant est equipe sur un vaisseau, il n'a pas de page de degats propres.
                    CardKind.COMMANDER -> CommanderPage(unit, unitWikiSections, onRuleClick)
                    else -> LegionUnitPage(unit, children, entries, unitWikiSections, onRuleClick,
                        woundsValue = stateMap[unit.instanceId]?.wounds,
                        onWoundsChange = { w ->
                            stateMap[unit.instanceId] = com.legionforge.app.data.nearby.SyncedUnitState(unit.instanceId, wounds = w)
                            if (isConnected) playSyncVm.sendState(listOf(stateMap[unit.instanceId]!!))
                        })
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
    // Sheet battle deck (battle cards v2, Legion) — 9 cartes 3+3+3 + mission.
    if (showBattleSheet) {
        BattleDeckSheet(
            cards = battleCards,
            deck = battleDeck,
            revealed = battleRevealed,
            onToggleDeck = { name ->
                val cat = battleCards.firstOrNull { it.name == name }?.category
                if (name in battleDeck) {
                    battleDeck = battleDeck - name
                    if (cat != null && battleRevealed[cat] == name) battleRevealed = battleRevealed - cat
                } else {
                    battleDeck = battleDeck + name
                    if (cat != null && battleRevealed[cat] == null) battleRevealed = battleRevealed + (cat to name)
                }
            },
            onCycleReveal = { cat ->
                val opts = battleCards.filter { it.category == cat && it.name in battleDeck }.map { it.name }.distinct()
                if (opts.isNotEmpty()) {
                    val cur = battleRevealed[cat]
                    val idx = opts.indexOf(cur).coerceAtLeast(0)
                    battleRevealed = battleRevealed + (cat to opts[(idx + 1) % opts.size])
                }
            },
            deckComplete = { cat -> battleCards.count { it.category == cat && it.name in battleDeck } >= 3 },
            onClose = { showBattleSheet = false }
        )
    }
    // Sheet du deck de cartes de commandement (Legion) — main officielle de 7.
    if (showCmdSheet) {
        val armyNames = remember { vm.entries.value.map { it.card.displayName() }.toSet() }
        CommandDeckSheet(
            cards = cmdCards.filter {
                (it.generic || it.faction == cmdFaction) &&
                (it.commanders.isEmpty() || it.commanders.any { c -> c in armyNames })
            },
            deck = commandDeck,
            played = playedCommands,
            onToggleDeck = { id -> commandDeck = if (id in commandDeck) commandDeck - id else commandDeck + id },
            onTogglePlayed = { id -> playedCommands = if (id in playedCommands) playedCommands - id else playedCommands + id },
            onClose = { showCmdSheet = false }
        )
    }
}

// ═══════════════════  LEGION  ═══════════════════════════
// Stats réels des cartes Legion v2 embarqués dans card.legionStats (JSON),
// générés par build_legion_v2.py depuis le bundle LegionHQ V2 (2.6).
internal data class LegionWeapon(val name: String, val rangeMin: Int, val rangeMax: Int,
                                val red: Int, val black: Int, val white: Int)
internal data class LegionStats(
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
internal object LegionStatsParser {
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

// ── Rendu lisible des conditions d'équipement Legion ─────────────────────────
// Le rulesText des upgrades LEGION est du JSON de "requirements" (conditions
// pour équiper l'upgrade sur une unité), PAS un texte d'effet. On le rend
// humainement lisible au lieu d'afficher le JSON brut. (Armada = déjà lisible.)
internal fun readableUpgradeText(rulesText: String?): String? {
    val rt = rulesText?.trim() ?: return null
    if (!rt.startsWith("{")) return stripBracketName(rt) // Armada : texte déjà lisible
    return try {
        val req = renderLegionRequirement(org.json.JSONObject(rt).opt("requirements")) ?: return null
        "Équipable sur : $req"
    } catch (_: Exception) { null }
}

private fun renderLegionRequirement(node: Any?): String? = when (node) {
    is org.json.JSONArray -> {
        val items = (0 until node.length()).map { node.get(it) }
        if (items.isNotEmpty() && items[0] is String) {
            val op = items[0] as String
            val rest = items.drop(1).mapNotNull { renderLegionRequirement(it) }
            when {
                op.equals("OR", true) -> rest.joinToString(" OU ")
                op.equals("AND", true) -> rest.joinToString(" ET ")
                op.equals("NOT", true) -> "pas ${rest.firstOrNull() ?: ""}"
                else -> rest.joinToString(" ")
            }
        } else items.mapNotNull { renderLegionRequirement(it) }.joinToString(" ")
    }
    is org.json.JSONObject -> {
        val parts = mutableListOf<String>()
        node.keys().forEach { k ->
            when (k) {
                "cardName" -> parts.add(node.optString("cardName"))
                "title" -> parts.add("\"${node.optString("title")}\"")
                "cardSubtype" -> parts.add(titleCase(node.optString("cardSubtype")))
                "faction" -> parts.add(titleCase(node.optString("faction")))
                "rank" -> parts.add(titleCase(node.optString("rank")))
                "forceAffinity" -> parts.add(if (node.optString("forceAffinity") == "light side") "côté clair" else "côté obscur")
                "upgradeBar" -> { val a = node.optJSONArray("upgradeBar"); if (a != null && a.length() > 0) parts.add("slot ${titleCase(a.getString(0))}") }
                "keywords" -> { val a = node.optJSONArray("keywords"); if (a != null && a.length() > 0) parts.add(a.getString(0)) }
                "stats" -> { val s = node.optJSONObject("stats"); if (s != null) parts.add("défense ${s.optString("defense")}") }
            }
        }
        parts.joinToString(" ").ifEmpty { null }
    }
    is String -> node
    else -> node?.toString()
}

private fun titleCase(s: String): String = s.replace('_', ' ').trim().replaceFirstChar { it.uppercase() }

// ── Cartes de commandement Legion ────────────────────────────────────────
internal data class LegionCommandCard(
    val id: String,
    val name: String,
    val pip: String,             // "1" | "2" | "3"
    val faction: String,
    val generic: Boolean = false,
    val commanders: List<String> = emptyList(),
    val keywords: List<String> = emptyList()
)

internal fun loadCommandCards(context: android.content.Context): List<LegionCommandCard> {
    return try {
        val s = context.assets.open("command_cards.json").bufferedReader().use { it.readText() }
        val arr = org.json.JSONObject(s).getJSONArray("cards")
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val cmdrs = buildList { val a = o.optJSONArray("commanders"); if (a != null) for (j in 0 until a.length()) add(a.getString(j)) }
                val kws = buildList { val a = o.optJSONArray("keywords"); if (a != null) for (j in 0 until a.length()) add(a.getString(j)) }
                add(LegionCommandCard(o.optString("id"), o.optString("name"), o.optString("pip"), o.optString("faction"), o.optBoolean("generic"), cmdrs, kws))
            }
        }
    } catch (_: Exception) { emptyList() }
}

internal data class LegionBattleCard(val category: String, val name: String)

internal fun loadBattleCards(context: android.content.Context): List<LegionBattleCard> {
    return try {
        val s = context.assets.open("battle_cards.json").bufferedReader().use { it.readText() }
        val arr = org.json.JSONObject(s).getJSONArray("cards")
        buildList { for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); add(LegionBattleCard(o.optString("category"), o.optString("name"))) } }
    } catch (_: Exception) { emptyList() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BattleDeckSheet(
    cards: List<LegionBattleCard>,
    deck: Set<String>,
    revealed: Map<String, String>,           // category -> carte révélée
    onToggleDeck: (String) -> Unit,
    onCycleReveal: (String) -> Unit,         // category -> passer à la carte suivante du deck
    deckComplete: (String) -> Boolean,       // category -> 3 sélectionnées
    onClose: () -> Unit
) {
    val order = listOf("objective" to "OBJECTIVE + MAP", "secondary" to "SECONDARY OBJECTIVE", "advantage" to "ADVANTAGE")
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF111827)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("BATTLE DECK (9 cartes)", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Officiel : 3 Objective(+Map) + 3 Secondary + 3 Advantage, sans doublon.", color = Color(0xFF9EACBC), style = MaterialTheme.typography.bodySmall)
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF2A3A4A)))
            order.forEach { (cat, label) ->
                val catCards = cards.filter { it.category == cat }
                if (catCards.isEmpty()) return@forEach
                val sel = deck.count { id -> catCards.any { it.name == id } }
                Text("$label  ($sel/3)", color = Color(0xFFB4BFCE), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    catCards.forEach { c ->
                        val inDeck = c.name in deck
                        val full = sel >= 3
                        Surface(
                            onClick = { if (inDeck || !full) onToggleDeck(c.name) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (inDeck) if (revealed[cat] == c.name) Color(0xFF5A2E2E) else Color(0xFF1E3A2A) else if (full) Color(0xFF11151D) else Color(0xFF192330),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.name, color = if (inDeck) Color.White else if (full) Color(0xFF5A6A7A) else Color(0xFFB4BFCE), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    if (inDeck && revealed[cat] == c.name) Text("RÉVÉLÉE", color = Color(0xFFFFC857), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                if (inDeck && revealed[cat] == c.name) Text("Mission en cours — touchez ↻ pour remplacer", color = Color(0xFFFFC7B7), fontSize = 8.sp)
                            }
                        }
                    }
                }
                // Mission : révéler / remplacer la carte courante de cette catégorie.
                val rev = revealed[cat]
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Mission → ", color = Color(0xFF9EACBC), fontSize = 10.sp)
                    Text(rev.takeIf { !it.isNullOrEmpty() } ?: "—", color = Color(0xFFFFC857), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    if (deckComplete(cat)) Button(onClick = { onCycleReveal(cat) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) { Text("↻", fontSize = 14.sp) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CommandDeckSheet(
    cards: List<LegionCommandCard>,
    deck: Set<String>,
    played: Set<String>,
    onToggleDeck: (String) -> Unit,
    onTogglePlayed: (String) -> Unit,
    onClose: () -> Unit
) {
    // Règle officielle v2 : main = 2×1-pip + 2×2-pip + 2×3-pip + Standing Orders (4-pip, obligatoire).
    val standingOrders = cards.firstOrNull { it.pip == "4" }
    val hand = deck + (standingOrders?.let { setOf(it.id) } ?: emptySet())
    fun selectedCount(pip: String): Int = cards.count { it.pip == pip && it.id in hand }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Color(0xFF111827)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("MAIN DE COMMANDEMENT", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Officielle : 2×1-pip + 2×2-pip + 2×3-pip + Standing Orders (4-pip).  ${hand.size}/7", color = Color(0xFF9EACBC), style = MaterialTheme.typography.bodySmall)
            // Standing Orders : toujours incluse, verrouillée.
            if (standingOrders != null) {
                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF1E3A2A), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("4", color = Color(0xFFFFC857), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(standingOrders.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("✓ obligatoire", color = Color(0xFF77D9A7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF2A3A4A)))
            listOf("1", "2", "3").forEach { pip ->
                val pipCards = cards.filter { it.pip == pip }
                if (pipCards.isEmpty()) return@forEach
                val sel = selectedCount(pip)
                Text("PIP $pip  ($sel/2)", color = Color(0xFFB4BFCE), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pipCards.forEach { c ->
                        val inDeck = c.id in deck
                        val isPlayed = c.id in played
                        val full = sel >= 2
                        Surface(
                            onClick = { if (inDeck || !full) onToggleDeck(c.id) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (inDeck) if (isPlayed) Color(0xFF5A2E2E) else Color(0xFF1E3A2A) else if (full) Color(0xFF11151D) else Color(0xFF192330),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$pip", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(6.dp))
                                    Text(c.name, color = if (full && !inDeck) Color(0xFF5A6A7A) else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    if (inDeck) {
                                        Surface(onClick = { onTogglePlayed(c.id) }, shape = RoundedCornerShape(6.dp), color = Color(0xFF2A3A4A)) {
                                            Text(if (isPlayed) "✓ JOUÉE" else "✓", Modifier.padding(horizontal = 7.dp, vertical = 3.dp), color = if (isPlayed) Color(0xFFFFC857) else Color(0xFF77D9A7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (inDeck) Text(if (isPlayed) "Jouée ce round" else "Dans la main — touchez pour retirer", color = if (isPlayed) Color(0xFFFFC7B7) else Color(0xFF9EACBC), fontSize = 9.sp)
                                else if (full) Text("Main complète (2/2)", color = Color(0xFF5A6A7A), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LegionUnitPage(unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}, woundsValue: Int? = null, onWoundsChange: (Int) -> Unit = {}) {
    val totalPts = unit.card.points + children.sumOf { it.card.points * it.quantity }
    val stats = remember(unit.instanceId) { LegionStatsParser.parse(unit.card.legionStats) }
    // Points de vie réels de l'unité = santé par figurine × nombre de figurines.
    // Tracker de VALEUR RESTANTE : démarre plein, descend sous les dégâts (comme la coque Armada).
    val maxHp = (stats?.health?.takeIf { it > 0 } ?: 1) * (stats?.miniCount?.takeIf { it > 0 } ?: 1)
    val wounds = woundsValue ?: maxHp
    var tokens by remember(unit.instanceId) { mutableStateOf(listOf<String>()) }
    // Courage de l'unité (depuis legionStats) : pilote les états Supprimé (> courage) / Paniqué (>= 2x courage).
    val courage = stats?.courage?.takeIf { it > 0 } ?: 1
    var suppressions by remember(unit.instanceId) { mutableIntStateOf(0) }
    val isWounded = wounds < maxHp
    val isSuppressed = suppressions >= courage
    val isPanicked = suppressions >= courage * 2
    var activeCrits by remember(unit.instanceId) { mutableStateOf(listOf<CritCard>()) }
    var showCard by remember(unit.instanceId) { mutableStateOf(false) }
    var usedUpgrades by remember(unit.instanceId) { mutableStateOf(setOf<String>()) }
    val defColor = if ((stats?.defenseDie ?: "w") == "r") Color(0xFFFF6B6B) else Color.White

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardBlock(unit, children, totalPts, wikiSections, onRuleClick, onCardClick = { showCard = true })
        // ── stats réelles de la carte (repliable) ──
        CollapsibleSection(stringResource(R.string.profile), initiallyExpanded = false) {
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
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                stats.keywords.forEach { kw ->
                                    val section = findWikiSection(wikiSections, kw)
                                    Surface(
                                        onClick = { if (section != null) onRuleClick(section) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2A3A4A)
                                    ) {
                                        Text(com.legionforge.app.data.i18n.I18n.keyword(kw), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = if (section != null) Color(0xFFFFC857) else Color(0xFF77D9A7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }           }
                                }
                                                        }
                                        }
                                        // ── suivi des blessures (valeur restante) (repliable) ──
        CollapsibleSection(stringResource(R.string.tracking), initiallyExpanded = false) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { BigCounter(stringResource(R.string.minis), wounds, maxHp, Color(0xFFFF6B6B), { if (wounds < maxHp) onWoundsChange(wounds + 1) }, { if (wounds > 0) onWoundsChange(wounds - 1) }) }
            HealthBar(wounds.toFloat() / maxHp, wounds, maxHp)
            // ── état de l'unité (auto-calculé depuis blessures + suppression / courage) ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StateChip(stringResource(R.string.legion_state_wounded), isWounded, Color(0xFFFFC857))
                StateChip(stringResource(R.string.legion_state_suppressed), isSuppressed, Color(0xFFFFB74D))
                StateChip(stringResource(R.string.legion_state_panicked), isPanicked, Color(0xFFFF6B6B))
            }
            // Compteur de suppression : alimente l'état Supprimé (>= courage) / Paniqué (>= 2x courage).
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.legion_suppression_label), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(onClick = { if (suppressions > 0) suppressions-- }, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(34.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) } }
                    Text("$suppressions / $courage", color = if (isPanicked) Color(0xFFFF6B6B) else if (isSuppressed) Color(0xFFFFB74D) else Color(0xFF77D9A7), fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Surface(onClick = { suppressions++ }, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(34.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) } }
                }
            }
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
        CollapsibleSection(stringResource(R.string.active_effects), initiallyExpanded = false) {
            EffectsPanel(
                effects = buildList {
                    children.forEach { c -> add(ActiveEffect(EffectType.UPGRADE, c.card.displayName(), readableUpgradeText(c.card.rulesText) ?: stringResource(R.string.upgrade_installed), c.instanceId, slot = c.card.upgradeSlots.firstOrNull(), pts = c.card.points).copy(used = usedUpgrades.contains(c.instanceId))) }
                    activeCrits.forEach { c -> add(ActiveEffect(EffectType.CRIT, c.name, c.effect, c.name)) }
                },
                onRemoveCrit = { activeCrits = activeCrits - it },
                                allCrits = legionCrits,
                                onToggleUsed = { eff ->
                                    if (eff.type == EffectType.UPGRADE) {
                                        usedUpgrades = if (eff.id in usedUpgrades) usedUpgrades - eff.id else usedUpgrades + eff.id
                                    }
                                }
                            )
        }
        CardZoomOverlay(unit.card, showCard) { showCard = false }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
internal fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF9EACBC), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable
internal fun surgeLabel(s: String?): String = when (s) { "h" -> stringResource(R.string.surge_crit); "a" -> stringResource(R.string.surge_hit); "b" -> stringResource(R.string.surge_block); "r" -> stringResource(R.string.surge_counter); else -> "—" }
internal fun diceText(red: Int, black: Int, white: Int): String {
    val parts = buildList { if (red > 0) add("R$red"); if (black > 0) add("N$black"); if (white > 0) add("B$white") }
    return if (parts.isEmpty()) "—" else parts.joinToString(" ")
}

// // ═══════════════════  ARMADA SHIP  ═══════════════════════
// Stats embarqués depuis le catalogue BSData ("fleet builder") dans card.shipStats (JSON).
internal data class ArmadaStats(
    val hull: Int = 0,
    val shieldFront: Int = 0,
    val shieldRear: Int = 0,
    val shieldPort: Int = 0,
    val shieldStarboard: Int = 0,
    val shieldPortAux: Int = 0,      // vaisseaux huge : 2e cadran bâbord
    val shieldStarboardAux: Int = 0, // vaisseaux huge : 2e cadran tribord
    val maxSpeed: Int = 1,
    val speed: Int = 3, // valeur fixe des squadrons
    val command: Int = 1, // niveau de commande = stock max de pions d'ordre
    val squadron: Int = 0,
    val engineering: Int = 0,
    val attackFront: List<Int> = emptyList(), // [rouge, bleu, noir]
    val attackRear: List<Int> = emptyList(),
    val attackPort: List<Int> = emptyList(),
    val attackStarboard: List<Int> = emptyList(),
    val attackPortAux: List<Int> = emptyList(),      // huge : dés bâbord auxiliaire
    val attackStarboardAux: List<Int> = emptyList(), // huge : dés tribord auxiliaire
    val antiSquadron: List<Int> = emptyList(), // [rouge, bleu, noir]
    val battery: List<Int> = emptyList(),      // [rouge, bleu, noir]
    val keywords: List<String> = emptyList(),
    val defenseTokens: List<String> = emptyList(),
    val speedChart: List<Map<String, Int>> = emptyList(), // liste de positions, chaque position = {vitesse: nb de clics}
    val size: String = "small" // huge = 2 cadrans de bouclier par côté (Executor, Starhawk)
)

internal object ArmadaStatsParser {
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
                        shieldPortAux = shield?.optInt("left_aux", 0) ?: 0,
                        shieldStarboardAux = shield?.optInt("right_aux", 0) ?: 0,
                        maxSpeed = o.optInt("maxSpeed", 1).coerceAtLeast(1),
                        command = o.optInt("command", 1).coerceAtLeast(1),
                        squadron = o.optInt("squadron", 0),
                        engineering = o.optInt("engineering", 0),
                        attackFront = arc("front"),
                        attackRear = arc("rear"),
                        attackPort = arc("left"),
                        attackStarboard = arc("right"),
                        attackPortAux = arc("left_aux"),
                        attackStarboardAux = arc("right_aux"),
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

// Matrice de manœuvres Armada : triangulaire (X = vitesse 1..maxSpeed, Y = position).
// Position 1 EN BAS, position max EN HAUT (inversé). Valeur : I = 1, II = 2, - = 0. Cases vides = bordeaux.
@Composable
private fun ManeuverMatrix(stats: ArmadaStats?, selectedSpeed: Int, onSelectSpeed: (Int) -> Unit) {
    if (stats?.speedChart?.isNotEmpty() != true) return
    val maxSpeed = stats.maxSpeed
    val maxPos = stats.speedChart.size.coerceAtLeast(1)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // En-tête : vitesses (X).
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(Modifier.width(30.dp)) // coin vide (label position)
            (1..maxSpeed).forEach { v ->
                val isSel = v == selectedSpeed
                Surface(
                    onClick = { onSelectSpeed(if (selectedSpeed == v) 0 else v) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSel) Color(0xFFFFC857) else Color(0xFF2A3A4A),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(Modifier.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                        Text("V$v", color = if (isSel) Color(0xFF0A0E15) else Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // Lignes : positions (Y), inversées (max en haut, 1 en bas).
        stats.speedChart.reversed().forEachIndexed { revIdx, posMap ->
            val posLabel = maxPos - revIdx
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                    Text("$posLabel", color = Color(0xFFFFC857), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                (1..maxSpeed).forEach { v ->
                    val n = posMap[v.toString()] ?: -1 // -1 = case vide (hors triangle)
                    val cellColor = if (n < 0) Color(0xFF800000) else Color.White
                    val cellText = when { n < 0 -> ""; n == 0 -> "-"; n == 1 -> "I"; n == 2 -> "II"; else -> "III" }
                    val isSel = v == selectedSpeed
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = cellColor,
                        modifier = Modifier.weight(1f).height(32.dp),
                        border = if (isSel) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFC857)) else null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(cellText, color = if (n < 0) Color(0xFF800000) else Color(0xFF1B2B4B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ArmadaShipPage(unit: ListEntry, children: List<ListEntry>, allEntries: List<ListEntry>, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}, hullValue: Int? = null, onHullChange: (Int) -> Unit = {}) {
    val totalPts = unit.card.points + children.sumOf { it.card.points * it.quantity }
    val stats = remember(unit.instanceId) { ArmadaStatsParser.parse(unit.card.shipStats, CardKind.ARMADA_SHIP) }
    // Coque + boucliers initialisés aux valeurs de base de la carte ; speed démarre à 2.
    // La coque est un tracker de valeur restante (démarre au hull de base, descend sous les dégâts).
    val maxHp = stats?.hull?.takeIf { it > 0 } ?: 15
    val hull = hullValue ?: maxHp
    var sF by remember(unit.instanceId) { mutableIntStateOf(stats?.shieldFront ?: 0) }
    var sR by remember(unit.instanceId) { mutableIntStateOf(stats?.shieldRear ?: 0) }
    var sP by remember(unit.instanceId) { mutableIntStateOf(stats?.shieldPort ?: 0) }
    var sS by remember(unit.instanceId) { mutableIntStateOf(stats?.shieldStarboard ?: 0) }
    // Vaisseaux "huge" (Executor, Starhawk) : 2 cadrans de bouclier par flanc (haut + bas).
    val isHuge = stats?.size == "huge"
    var sP2 by remember(unit.instanceId) { mutableIntStateOf(if (isHuge) (stats?.shieldPortAux ?: 0) else 0) }
    var sS2 by remember(unit.instanceId) { mutableIntStateOf(if (isHuge) (stats?.shieldStarboardAux ?: 0) else 0) }
    val maxShield = 9
    val maxSpeed = stats?.maxSpeed ?: 3
    var speed by remember(unit.instanceId) { mutableIntStateOf(2) }
    // Colonne de vitesse sélectionnée dans la matrice de manoeuvres (défaut = 2, vitesse de départ des vaisseaux).
    var selectedSpeed by remember(unit.instanceId) { mutableIntStateOf(2) }
    var activeCrits by remember(unit.instanceId) { mutableStateOf(listOf<CritCard>()) }
    var showCritSelector by remember(unit.instanceId) { mutableStateOf(false) }
    var showCard by remember(unit.instanceId) { mutableStateOf(false) }
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
        // ── ship card (zone du haut compacte, reprise de la vue condensée) ──
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardArtwork(unit.card, Modifier.width(52.dp).clickable { showCard = true })
                    Column(Modifier.weight(1f)) { Text(unit.card.displayName(), color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${stringResource(R.string.kind_ship)}  •  ${unit.card.factionId.replace('-', ' ').replaceFirstChar { it.uppercase() }}", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall) }
                    if (isHuge) Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF3A2050)) { Text("HUGE", Modifier.padding(horizontal = 6.dp, vertical = 1.dp), color = Color(0xFFD7A6FF), fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                    Text("${unit.card.points}", color = Color(0xFFFFC857), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (stats != null) ShipStatsBlock(stats)
                else if (!unit.card.rulesText.isNullOrBlank()) ClickableRulesText(unit.card.rulesText, wikiSections, onRuleClick, color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall, maxLines = 6)
                // ── résumé coque (avec +/-) + vitesse + bouton +DGT CRIT (toujours visible) ──
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    HullCounter(hull, maxHp, { if (hull < maxHp) onHullChange(hull + 1) }, { if (hull > 0) onHullChange(hull - 1) })
                    StatChip(stringResource(R.string.stat_speed), speed, Color(0xFF77D9A7), modifier = Modifier.weight(1f), display = "$speed/$maxSpeed")
                    // Bouton +DGT CRIT : ajoute une carte de dégât critique (toujours visible).
                    Surface(onClick = { showCritSelector = true }, shape = RoundedCornerShape(10.dp), color = Color(0xFF5A2020)) {
                        Text(stringResource(R.string.add_crit), Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = Color(0xFFFF6B6B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Sélecteur de dégât critique (ouvert par le bouton +DGT CRIT).
                CritSelectorDropdown(armadaCrits, showCritSelector, { showCritSelector = false }, { c -> activeCrits = activeCrits + c; showCritSelector = false })
            }
        }
                // ── matrice de manœuvres (repliable, repliée par défaut) ; pilote la vitesse ──
                CollapsibleSection(stringResource(R.string.maneuver_matrix), initiallyExpanded = false) {
                    if (stats?.speedChart?.isNotEmpty() == true) {
                        ManeuverMatrix(stats, selectedSpeed, { v ->
                            selectedSpeed = if (selectedSpeed == v) 0 else v
                            if (selectedSpeed != 0) speed = selectedSpeed
                        })
                    } else {
                        Surface(shape = RoundedCornerShape(9.dp), color = Color(0xFF1F2C3D), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.cadrans), color = Color(0xFF9EACBC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text("🛡 ${sF}/${sP}/${sS}/${sR}", color = Color(0xFF4FC3F7), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("⚄ ${(stats?.attackFront?.sum() ?: 0)}", color = Color(0xFFFF6B6B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                // ── command dial (roue de commandement) (en haut, repliable) ──
                CollapsibleSection(stringResource(R.string.command_wheel), initiallyExpanded = false) {
                    CommandDialCard(
                        selected = commandOrder,
                        onSelect = { commandOrder = if (commandOrder == it) null else it },
                        wikiSections = wikiSections,
                        onRuleClick = onRuleClick
                    )
                }
                // ── pions d'ordre (stock) (repliable) ──
                CollapsibleSection(stringResource(R.string.order_tokens), initiallyExpanded = false) {
                    OrderTokensCard(
                        orderTokens = orderTokens,
                        maxStock = maxOrderStock,
                        onIncOrder = { name ->
                            if (orderTokens.values.sum() < maxOrderStock) orderTokens = orderTokens + (name to (orderTokens[name] ?: 0) + 1)
                        },
                        onDecOrder = { name -> orderTokens = orderTokens + (name to ((orderTokens[name] ?: 0) - 1).coerceAtLeast(0)) },
                        wikiSections = wikiSections,
                        onRuleClick = onRuleClick
                    )
                }
                // ── defense tokens (repliable) ──
                CollapsibleSection(stringResource(R.string.defense_tokens), initiallyExpanded = false) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        defTokenStates.forEachIndexed { i, (def, _) ->
                            val key = "${def.name}_$i"
                            val used = defTokens[key] ?: false
                            DefenseTokenDisc(def, used, onClick = { defTokens = defTokens + (key to !used) }, onWikiClick = { findWikiSection(wikiSections, def.label)?.let(onRuleClick) })
                        }
                    }
                }
        // ── effects panel (améliorations remontées au-dessus de la roue de commandement) (repliable) ──
        CollapsibleSection(stringResource(R.string.active_effects), initiallyExpanded = false) {
            EffectsPanel(
                effects = buildList {
                    if (commander != null) add(ActiveEffect(EffectType.COMMANDER, commander.card.displayName(), stripBracketName(commander.card.rulesText ?: stringResource(R.string.fleet_commander)), "cmd_${commander.instanceId}", pts = commander.card.points)
                        .copy(used = usedUpgrades.contains("cmd")))
                    children.forEach { c ->
                        if (c.card.kind == CardKind.COMMANDER || (c.card.kind == CardKind.ARMADA_UPGRADE && ArmadaSlot.COMMANDER in c.card.upgradeSlots)) return@forEach
                        add(ActiveEffect(EffectType.UPGRADE, c.card.displayName(), stripBracketName(c.card.rulesText ?: stringResource(R.string.upgrade_installed)), c.instanceId, slot = c.card.upgradeSlots.firstOrNull(), pts = c.card.points)
                            .copy(used = usedUpgrades.contains(c.instanceId)))
                    }
                    activeCrits.forEach { c -> add(ActiveEffect(EffectType.CRIT, c.name, c.effect, "crit_${c.name}")) }
                },
                onRemoveCrit = { activeCrits = activeCrits - it },
                allCrits = armadaCrits
            )
        }
        // ── cadrans (boucliers & coque) + dés d'attaque par côté (repliable) ──
        CollapsibleSection(stringResource(R.string.shields_hull), initiallyExpanded = false) {
                // ── cadrans des 4 arcs en croix (comme sur la carte officielle) : rectangle de dés + cercle de bouclier ──
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // AVANT : rectangle de dés AU-DESSUS du cercle de bouclier.
                    ArcCadran(ArcPos.ABOVE, listOf(stats?.attackFront), listOf(sF), { if (sF < maxShield) sF++ }, { if (sF > 0) sF-- }, { if (sF < maxShield) sF++ }, { if (sF > 0) sF-- })
                    // Ligne centrale : BAB (gauche) | TRIB (droite). Vaisseaux huge = 2 cadrans par flanc.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
                        ArcCadran(ArcPos.LEFT,
                            if (isHuge) listOf(stats?.attackPort, stats?.attackPortAux) else listOf(stats?.attackPort),
                            if (isHuge) listOf(sP, sP2) else listOf(sP),
                            { if (sP < maxShield) sP++ }, { if (sP > 0) sP-- },
                            { if (sP2 < maxShield) sP2++ }, { if (sP2 > 0) sP2-- })
                        ArcCadran(ArcPos.RIGHT,
                            if (isHuge) listOf(stats?.attackStarboard, stats?.attackStarboardAux) else listOf(stats?.attackStarboard),
                            if (isHuge) listOf(sS, sS2) else listOf(sS),
                            { if (sS < maxShield) sS++ }, { if (sS > 0) sS-- },
                            { if (sS2 < maxShield) sS2++ }, { if (sS2 > 0) sS2-- })
                    }
                    // ARRIERE : rectangle de dés EN DESSOUS du cercle de bouclier.
                    ArcCadran(ArcPos.BELOW, listOf(stats?.attackRear), listOf(sR), { if (sR < maxShield) sR++ }, { if (sR > 0) sR-- }, { if (sR < maxShield) sR++ }, { if (sR > 0) sR-- })
                }
                HorizontalDivider(color = Color(0xFF2A3A4A))
                                // Résumé coque/vitesse déplacé en haut (header ship card) ; gros compteurs rouges retirés pour compacter.
                                // La matrice de manœuvres est désormais dans le header (remplace la ligne "Cadrans").
                        }
                        CardZoomOverlay(unit.card, showCard) { showCard = false }
        Spacer(Modifier.height(20.dp))
    }
}
// ═══════════════════  COMMANDER (pas de degats critiques, equipe sur le flagship)  ═══════════════════
@Composable
internal fun CommanderPage(unit: ListEntry, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}) {
    var showCard by remember(unit.instanceId) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CardBlock(unit, emptyList(), unit.card.points, wikiSections, onRuleClick, onCardClick = { showCard = true })
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.fleet_command), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
        CardZoomOverlay(unit.card, showCard) { showCard = false }
        Spacer(Modifier.height(20.dp))
    }
}

// ═══════════════════  SQUADRON  ═══════════════════════════
@Composable
internal fun ArmadaSquadronPage(unit: ListEntry, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}) {
    var showCard by remember(unit.instanceId) { mutableStateOf(false) }
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
        CardBlock(unit, emptyList(), unit.card.points, wikiSections, onRuleClick, onCardClick = { showCard = true })
        CollapsibleSection(stringResource(R.string.squadron_tracking), initiallyExpanded = false) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                BigCounter(stringResource(R.string.hull), hull, maxHp, Color(0xFFFF6B6B), { if (hull < maxHp) hull++ }, { if (hull > 0) hull-- })
                Spacer(Modifier.width(20.dp))
                // Jeton d'activation/désactivation de l'escadron (à la place du coût, redondant avec le cadre du haut).
                SquadronActivationToken(
                    activated = activated,
                    onToggle = { activated = !activated },
                    onWikiClick = { findWikiSection(wikiSections, "activation")?.let(onRuleClick) }
                )
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
        if (defTokenStates.isNotEmpty()) {
            CollapsibleSection(stringResource(R.string.defense_tokens), initiallyExpanded = false) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    defTokenStates.forEachIndexed { i, (def, _) ->
                        val key = "${def.name}_$i"
                        val used = defTokens[key] ?: false
                        DefenseTokenDisc(def, used, onClick = { defTokens = defTokens + (key to !used) }, onWikiClick = { findWikiSection(wikiSections, def.label)?.let(onRuleClick) })
                    }
                }
            }
        }
        CardZoomOverlay(unit.card, showCard) { showCard = false }
        Spacer(Modifier.height(20.dp))
    }
}

// ═══════════════════  SHARED COMPONENTS  ═══════════════════

// Overlay zoom : la vraie carte en grand, au-dessus du contenu, ouverte au clic sur la miniature.
@Composable
internal fun CardZoomOverlay(card: CardDefinition, show: Boolean, onDismiss: () -> Unit) {
    if (!show) return
    val source: Any? = card.imageAssetPath?.let { "file:///android_asset/$it" } ?: card.imageUrl
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(0.96f).verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF192330)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(card.displayName(), color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (source != null) {
                    AsyncImage(model = source, contentDescription = stringResource(R.string.icon_desc, card.displayName()),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp), contentScale = ContentScale.Fit)
                }
                Surface(onClick = onDismiss, shape = RoundedCornerShape(8.dp), color = Color(0xFF2A3A4A)) {
                    Text("✕", Modifier.padding(horizontal = 14.dp, vertical = 6.dp), color = Color(0xFF9EACBC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun CardBlock(unit: ListEntry, children: List<ListEntry>, totalPts: Int, wikiSections: List<WikiSectionEntity> = emptyList(), onRuleClick: (WikiSectionEntity) -> Unit = {}, onCardClick: (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                if (onCardClick != null) CardArtwork(unit.card, Modifier.width(64.dp).clickable { onCardClick() })
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

// Section repliable : en-tete cliquable (titre + chevron) + contenu masque/deplie.
// Permet de compacter le mode partie sur un ecran de telephone : les infos secondaires
// (roue de commandement, effets, profil) sont repliees par defaut, le suivi principal reste visible.
@Composable
internal fun CollapsibleSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color(0xFFFFC857), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(if (expanded) "\u25B2" else "\u25BC", color = Color(0xFF9EACBC), fontSize = 12.sp)
            }
            if (expanded) {
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
            }
        }
    }
}

@Composable
internal fun EffectsPanel(
    effects: List<ActiveEffect>,
    onRemoveCrit: (CritCard) -> Unit,
    allCrits: List<CritCard>,
    onToggleUsed: (ActiveEffect) -> Unit = {}
) {
    var selectedEffect by remember { mutableStateOf<ActiveEffect?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    onClick = { onToggleUsed(eff) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = when { eff.used -> Color(0xFF1A1A2A); eff.type == EffectType.CRIT -> Color(0xFF3B2224); eff.type == EffectType.ABILITY -> Color(0xFF1E3A2A); else -> Color(0xFF1E2A3A) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                    Column(Modifier.padding(8.dp)) {
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
                                                                Surface(onClick = { selectedEffect = if (selectedEffect == eff) null else eff }, shape = RoundedCornerShape(6.dp), color = Color(0xFF2A3A4A)) {
                                                                    Text(if (selectedEffect == eff) "\u25B2" else "\u25BC", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF9EACBC), fontSize = 12.sp)
                                                                }
                            }
                        }
                        // Expanded description
                                                if (selectedEffect == eff) {
                                                    Spacer(Modifier.height(8.dp))
                                                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = Color(0xFF0A0E15)) {
                                                        Text(stripLeadingName(eff.desc, eff.label), Modifier.padding(12.dp), color = Color(0xFFB4BFCE), style = MaterialTheme.typography.bodySmall)
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
internal fun CritSelectorDropdown(crits: List<CritCard>, expanded: Boolean, onDismiss: () -> Unit, onSelect: (CritCard) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        crits.forEach { crit ->
            DropdownMenuItem(text = { Column { Text(crit.name, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(crit.effect.take(60)+"...", fontSize = 11.sp, color = Color.Gray) } }, onClick = { onSelect(crit) })
        }
    }
}

@Composable
internal fun HealthBar(ratio: Float, current: Int, max: Int) {
    val barColor = when { ratio > 0.66f -> Color(0xFF77D9A7); ratio > 0.33f -> Color(0xFFFFC857); else -> Color(0xFFFF6B6B) }
    LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = barColor, trackColor = Color(0xFF2A3A4A))
    Box(Modifier.fillMaxWidth()) { Text("$current / $max", color = barColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterEnd)) }
}

@Composable
internal fun DefenseTokenDisc(def: ArmadaDefenseToken, used: Boolean, onClick: () -> Unit, onWikiClick: (() -> Unit)? = null) {
    val ringColor = if (used) Color(0xFFFF6B6B) else Color(0xFF77D9A7)
    // Les jetons de defense officiels (render fandom, 100x52) : vert = PRET, rouge = UTILISE.
    val tokenFile = "tokens/${def.name.lowercase()}_${if (used) "exhausted" else "ready"}.webp"
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
        }
        // Le libellé du jeton est cliquable vers le wiki (plus de bouton "i").
        Text(
            def.label,
            color = ringColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = if (onWikiClick != null) Modifier.clickable { onWikiClick() } else Modifier
        )
    }
}

// ── stats du vaisseau (au lieu du JSON brut) ─────────────
@Composable
internal fun ShipStatsBlock(stats: ArmadaStats) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatChip(stringResource(R.string.stat_command), stats.command, Color(0xFFFFC857), modifier = Modifier.weight(1f))
            StatChip(stringResource(R.string.stat_squadron), stats.squadron, Color(0xFF4FC3F7), modifier = Modifier.weight(1f))
            StatChip(stringResource(R.string.stat_engineering), stats.engineering, Color(0xFF77D9A7), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
internal fun StatChip(label: String, value: Int, color: Color, modifier: Modifier = Modifier, display: String? = null) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f), modifier = modifier) {
        Column(Modifier.padding(vertical = 6.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(display ?: "$value", color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, color = color.copy(alpha = 0.8f), fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

// ── dés d'attaque : UN losange par dé (comme la carte officielle), sans chiffre ──
// Losange plein de couleur, un par dé de cette couleur.
@Composable
internal fun DiceDiamond(color: Color, size: Dp = 14.dp) {
    Canvas(Modifier.size(size)) {
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
}

// Dés d'attaque d'un arc : UN losange par dé, ordre ROUGE, BLEU, NOIR.
// vertical=false (AV/ARR) : rangée horizontale, 2 lignes si >2 dés.
// vertical=true (BAB/TRIB) : 2 colonnes pour gagner de la largeur.
@Composable
internal fun AttackDiceRow(dice: List<Int>?, vertical: Boolean = false) {
    if (dice.isNullOrEmpty() || dice.all { it <= 0 }) return
    val red = dice.getOrNull(0) ?: 0
    val blue = dice.getOrNull(1) ?: 0
    val black = dice.getOrNull(2) ?: 0
    val total = blue + red + black
    // Ordre d'affichage : rouge, bleu, noir. Un losange par dé.
    val colors = listOf(Color(0xFFFF6B6B), Color(0xFF4FC3F7), Color(0xFF3A3A4A))
    val counts = listOf(red, blue, black)
    // Construit la liste des losanges (un par dé) dans l'ordre rouge/bleu/noir.
    val all = buildList { counts.forEachIndexed { i, c -> repeat(c) { add(colors[i]) } } }
    if (total <= 2) {
        // Une seule rangée.
        if (vertical) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                all.forEach { DiceDiamond(it) }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                all.forEach { DiceDiamond(it) }
            }
        }
    } else {
        // Répartir en 2 groupes équilibrés (moitié/moitié).
        val g1 = mutableListOf<Color>()
        val g2 = mutableListOf<Color>()
        counts.forEachIndexed { i, c ->
            val half = c / 2
            val rem = c - half
            repeat(half) { g1.add(colors[i]) }
            repeat(rem) { g2.add(colors[i]) }
        }
        if (vertical) {
            // 2 colonnes côte à côte (étroit).
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) { g1.forEach { DiceDiamond(it) } }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) { g2.forEach { DiceDiamond(it) } }
            }
        } else {
            // 2 lignes empilées.
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) { g1.forEach { DiceDiamond(it) } }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) { g2.forEach { DiceDiamond(it) } }
            }
        }
    }
}

// ── cadrans des arcs (imité de la carte officielle) ──────────
// Position du rectangle de dés par rapport au cercle de bouclier.
internal enum class ArcPos { ABOVE, BELOW, LEFT, RIGHT }

// Cadran d'un arc : rectangle(s) de dés positionné(s) autour du/des cercle(s) de bouclier.
// diceList = liste des dés par cadran (1 pour normal, 2 pour huge). values = valeurs de bouclier par cadran.
// onInc1/onDec1 = 1er cadran, onInc2/onDec2 = 2e.
@Composable
internal fun ArcCadran(pos: ArcPos, diceList: List<List<Int>?>, values: List<Int>, onInc1: () -> Unit, onDec1: () -> Unit, onInc2: () -> Unit, onDec2: () -> Unit) {
    val isFlank = pos == ArcPos.LEFT || pos == ArcPos.RIGHT
    val rects: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DiceRect(diceList.getOrNull(0), isFlank)
            if (diceList.size > 1) DiceRect(diceList.getOrNull(1), isFlank)
        }
    }
    val shields: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CircleShield(values.getOrElse(0) { 0 }, onInc1, onDec1)
            if (values.size > 1) CircleShield(values.getOrElse(1) { 0 }, onInc2, onDec2)
        }
    }
    when (pos) {
        ArcPos.ABOVE -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { rects(); shields() }
        ArcPos.BELOW -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) { shields(); rects() }
        ArcPos.LEFT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { rects(); shields() }
        ArcPos.RIGHT -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { shields(); rects() }
    }
}

// Rectangle blanc contenant les dés d'attaque en losanges de couleur (comme le cadran de la carte).
@Composable
internal fun DiceRect(dice: List<Int>?, vertical: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFE8EDF5),   // fond blanc cassé du rectangle sur la carte
        modifier = Modifier.defaultMinSize(minWidth = if (vertical) 30.dp else 44.dp, minHeight = 30.dp)
    ) {
        Box(Modifier.padding(horizontal = 6.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
            if (dice.isNullOrEmpty() || dice.all { it <= 0 }) {
                Text("—", color = Color(0xFF9AA7B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else {
                AttackDiceRow(dice, vertical)
            }
        }
    }
}

// Cercle blanc à fine bordure bleue contenant la valeur de bouclier (comme sur la carte officielle).
@Composable
internal fun CircleShield(value: Int, onInc: () -> Unit, onDec: () -> Unit) {
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
    }
}

// ── roue de commandement (dial) ──────────
@Composable
internal fun CommandDialCard(
    selected: ArmadaCommandOrder?,
    onSelect: (ArmadaCommandOrder) -> Unit,
    wikiSections: List<WikiSectionEntity> = emptyList(),
    onRuleClick: (WikiSectionEntity) -> Unit = {}
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // N'affiche QUE l'ordre sélectionné (pastille compacte) ; sinon un selecteur compact par tap.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                if (selected != null) {
                    CommandDialButton(order = selected!!, selected = true, onClick = { onSelect(selected!!) }, modifier = Modifier.size(width = 96.dp, height = 84.dp))
                } else {
                    // Aucun ordre choisi : 4 pions cliquables pour sélectionner.
                    ArmadaCommandOrder.entries.forEach { order ->
                        CommandDialButton(order = order, selected = false, onClick = { onSelect(order) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── pions d'ordre (stock) ──────────
@Composable
internal fun OrderTokensCard(
    orderTokens: Map<String, Int>,
    maxStock: Int,
    onIncOrder: (String) -> Unit,
    onDecOrder: (String) -> Unit,
    wikiSections: List<WikiSectionEntity> = emptyList(),
    onRuleClick: (WikiSectionEntity) -> Unit = {}
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                // Compteur de pions compact (le titre est déjà dans l'en-tête du menu repliable).
                Text(stringResource(R.string.orders_stock, orderTokens.values.sum(), maxStock), color = Color(0xFF77D9A7), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
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
internal fun CommandDialButton(order: ArmadaCommandOrder, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
internal fun OrderTokenDisc(order: ArmadaCommandOrder, count: Int, onWikiClick: () -> Unit, onInc: () -> Unit, onDec: () -> Unit) {
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
internal fun SquadronActivationToken(activated: Boolean, onToggle: () -> Unit, onWikiClick: () -> Unit) {
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
        }
        // Le texte ACTIF/INACTIF est cliquable vers le wiki (plus de bouton "i").
        Text(
            if (activated) "ACTIF" else "INACTIF",
            color = faceLight,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onWikiClick() }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TokenSection(tokens: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    // Jetons d'action Legion v2 : Aim (jaune), Dodge (violet), Surge (bleu), Standby (orange).
    val allTokens = listOf(
        LegionToken(stringResource(R.string.legion_token_aim), Color(0xFFFFC857), "\u25CE"),
        LegionToken(stringResource(R.string.legion_token_dodge), Color(0xFF9B6DFF), "\u25CC"),
        LegionToken(stringResource(R.string.legion_token_surge), Color(0xFF4FC3F7), "\u26A1"),
        LegionToken(stringResource(R.string.legion_token_standby), Color(0xFFFF8A65), "\u25C9"),
        LegionToken(stringResource(R.string.legion_token_observation), Color(0xFF90CAF9), "\u25C8"),
        LegionToken(stringResource(R.string.legion_token_advantage), Color(0xFF77D9A7), "\u25B2"),
        LegionToken(stringResource(R.string.legion_token_smoke), Color(0xFF9EACBC), "\u25CF"),
        LegionToken(stringResource(R.string.legion_token_charge), Color(0xFFFFB74D), "\u2694")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.tokens_markers), color = Color(0xFF9EACBC), style = MaterialTheme.typography.labelLarge)
        // Disques de jetons (vrais tokens) : retour à la ligne automatique si trop nombreux.
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            allTokens.forEach { t -> LegionTokenDisc(t.label, t.color, t.symbol, onClick = { onAdd(t.label) }) }
        }
        if (tokens.isNotEmpty()) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tokens.forEach { t ->
                    val meta = allTokens.firstOrNull { it.label == t }
                    Surface(onClick = { onRemove(t) }, shape = RoundedCornerShape(8.dp), color = Color(0xFF3B2224)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(meta?.symbol ?: "\u25A3", color = meta?.color ?: Color(0xFFFFC7B7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text("$t  \u2715", color = Color(0xFFFFC7B7), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Text(stringResource(R.string.tap_token), color = Color(0xFF5A6A7A), style = MaterialTheme.typography.labelSmall)
    }
}

// Jeton d'action Legion : disque coloré avec symbole, cliquable pour l'ajouter.
@Composable
internal fun LegionTokenDisc(label: String, color: Color, symbol: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = color.copy(alpha = 0.22f),
            border = androidx.compose.foundation.BorderStroke(2.dp, color),
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) { Text(symbol, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// Modèle d'un jeton d'action Legion (label localisé + couleur + symbole).
internal data class LegionToken(val label: String, val color: Color, val symbol: String)

// Badge d'état auto (Blessé / Supprimé / Paniqué) : actif = coloré, sinon grisé.
@Composable
internal fun StateChip(label: String, active: Boolean, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = if (active) color.copy(alpha = 0.22f) else Color(0xFF1E2A3A)) {
        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = if (active) color else Color(0xFF5A6A7A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun BigCounter(label: String, value: Int, max: Int, color: Color, onInc: () -> Unit, onDec: () -> Unit) {
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

// Compteur de coque compact avec +/- (pilote la coque actuelle, toujours visible dans le header).
@Composable
internal fun HullCounter(value: Int, max: Int, onInc: () -> Unit, onDec: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.hull), color = Color(0xFFFF8A80), fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(onClick = onDec, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(26.dp)) { Box(contentAlignment = Alignment.Center) { Text("-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
            Text("$value/$max", color = Color(0xFFFF8A80), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
            Surface(onClick = onInc, shape = CircleShape, color = Color(0xFF2A3A4A), modifier = Modifier.size(26.dp)) { Box(contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
        }
    }
}
