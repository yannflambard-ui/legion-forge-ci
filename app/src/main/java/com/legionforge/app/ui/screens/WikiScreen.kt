package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.data.model.WikiSectionEntity
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel

/** Wiki des règles officielles (Armada + Legion) : recherche par mot-clé sur la
 *  page de garde, affichage de chaque section. Le texte reprend les PDF officiels
 *  Atomic Mass Games. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun WikiScreen(
    onBack: () -> Unit,
    vm: ArmyBuilderViewModel
) {
    var query by remember { mutableStateOf("") }
    val sections by vm.wikiSections.collectAsState()
    val results by vm.wikiSearchResults.collectAsState()
    val searching by vm.wikiSearching.collectAsState()
    var selected by remember { mutableStateOf<WikiSectionEntity?>(null) }

    LaunchedEffect(Unit) { vm.loadAllWiki() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("WIKI DES RÈGLES", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { TextButton(onClick = onBack) { Text("<") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E15))
        )
    }) { pad ->
        Column(Modifier.fillMaxSize().background(Color(0xFF0A0E15)).padding(pad).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Barre de recherche par mot-clé
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; vm.searchWiki(it) },
                placeholder = { Text("Mot-clé : boucliers, activation, attaque, vitesse…", color = Color(0xFF5A6A7A), fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WikiGameChip("LEGION", GameSystem.LEGION_V2, Modifier.weight(1f), vm)
                WikiGameChip("ARMADA", GameSystem.ARMADA_V15, Modifier.weight(1f), vm)
            }
            when {
                searching -> Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFFFC857), strokeWidth = 2.dp) }
                query.isNotBlank() && results.isEmpty() -> Text("Aucune section pour « $query »", color = Color(0xFF5A6A7A), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 12.dp))
                query.isNotBlank() -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(results) { section -> WikiSectionRow(section, onClick = { selected = section }) }
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                    items(sections) { section -> WikiSectionRow(section, onClick = { selected = section }) }
                }
            }
        }
    }

    // Popup de section : affiche le texte officiel de la règle.
    selected?.let { section ->
        RulePopup(section = section, onClose = { selected = null })
    }
}

@Composable
private fun WikiGameChip(label: String, system: GameSystem, modifier: Modifier = Modifier, vm: ArmyBuilderViewModel) {
    var active by remember(system) { mutableStateOf(false) }
    FilterChip(
        selected = active,
        onClick = {
            active = !active
            if (active) vm.loadWiki(system) else vm.loadAllWiki()
        },
        modifier = modifier,
        label = { Text(label, fontSize = 12.sp) }
    )
}

@Composable
private fun WikiSectionRow(section: WikiSectionEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF192330))) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(section.title, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(if (section.gameSystem == GameSystem.LEGION_V2.name) "LEGION" else "ARMADA", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Text(section.content.replace('\n', ' ').take(90) + "…", color = Color(0xFF9EACBC), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

/** Popup de règle : fenêtre superposée (même style que les filtres du catalogue)
 *  affichant le texte officiel d'une section du wiki. */
@Composable
fun RulePopup(section: WikiSectionEntity, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF192330)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(section.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (section.gameSystem == GameSystem.LEGION_V2.name) "LEGION V2" else "ARMADA V1.5", color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = onClose) { Text("✕", color = Color.White, fontSize = 18.sp) }
                }
                HorizontalDivider(color = Color(0xFF2A3A4A))
                Text(
                    section.content,
                    color = Color(0xFFD5DCE6),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

/** Texte de règles avec mots-clés du wiki cliquables : clic sur un mot-clé
 *  (ex: "boucliers", "activation", "attaque") -> onRuleClick(section) pour
 *  afficher le point de règle officiel dans une popup superposée. */
@Composable
fun ClickableRulesText(
    text: String,
    wikiSections: List<WikiSectionEntity>,
    onRuleClick: (WikiSectionEntity) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFB4BFCE),
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall,
    maxLines: Int = Int.MAX_VALUE
) {
    // Construire une map mot-clé (minuscule) -> section pour la détection.
    val kwToSection = remember(wikiSections) {
        buildMap {
            wikiSections.forEach { s ->
                s.keywordList().forEach { kw ->
                    if (kw.length >= 3) put(kw.lowercase(), s)
                }
                // le titre lui-même est un mot-clé
                if (s.title.length >= 3) put(s.title.lowercase(), s)
            }
        }
    }
    val annotated = buildAnnotatedString {
        // Découper le texte en tokens (mots + ponctuation) pour détecter les mots-clés.
        val regex = Regex("""[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ'\-]*""")
        var last = 0
        regex.findAll(text).forEach { m ->
            if (m.range.first > last) append(text.substring(last, m.range.first))
            val word = m.value
            val section = kwToSection[word.lowercase()]
            if (section != null) {
                pushStringAnnotation(tag = "rule", annotation = section.id)
                withStyle(SpanStyle(color = Color(0xFFFFC857), fontWeight = FontWeight.Bold)) {
                    append(word)
                }
                pop()
            } else {
                append(word)
            }
            last = m.range.last + 1
        }
        if (last < text.length) append(text.substring(last))
    }
    ClickableText(
        text = annotated,
        style = style.copy(color = color),
        maxLines = maxLines,
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "rule", start = offset, end = offset)
                .firstOrNull()?.let { ann ->
                    kwToSection.values.firstOrNull { it.id == ann.item }?.let(onRuleClick)
                }
        }
    )
}
