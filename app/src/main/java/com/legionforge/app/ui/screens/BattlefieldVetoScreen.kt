package com.legionforge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Données de l'étape 2 : sélection du champ de bataille (véto) ─────────────
// 9 cartes réparties en 3 catégories (3 Déploiements, 3 Objectifs, 3 Conditions).
internal data class BattlefieldCard(val id: String, val name: String, val category: String)

internal val battlefieldDeck = listOf(
    // Déploiements
    BattlefieldCard("dep1", "Déploiement Avancé", "Déploiement"),
    BattlefieldCard("dep2", "Lignes de Front", "Déploiement"),
    BattlefieldCard("dep3", "Flancs Dégagés", "Déploiement"),
    // Objectifs
    BattlefieldCard("obj1", "Récupérer les Données", "Objectif"),
    BattlefieldCard("obj2", "Intercepter les Signaux", "Objectif"),
    BattlefieldCard("obj3", "Défendre la Position", "Objectif"),
    // Conditions
    BattlefieldCard("cond1", "Terrain Hostile", "Condition"),
    BattlefieldCard("cond2", "Brouillard Dense", "Condition"),
    BattlefieldCard("cond3", "Vents Violents", "Condition")
)

// ── Écran 2 : sélection du champ de bataille (système de véto) ──────────────
// Les joueurs alternent pour bannir des cartes. Le Joueur Bleu commence.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattlefieldVetoScreen(
    onBack: () -> Unit,
    onValidate: (banned: List<String>) -> Unit
) {
    // État : cartes bannies (set d'ids) + joueur actif (true = Bleu, false = Rouge).
    var banned by remember { mutableStateOf(setOf<String>()) }
    var blueTurn by remember { mutableStateOf(true) }

    val activeColor = if (blueTurn) Color(0xFF3B82F6) else Color(0xFFEF4444)
    val activeLabel = if (blueTurn) "Joueur Bleu" else "Joueur Rouge"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Champ de bataille", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0E15))
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().background(Color(0xFF0A0E15)).padding(pad).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header : indicateur du tour de rôle ──
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = activeColor.copy(alpha = 0.18f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).background(activeColor, RoundedCornerShape(50)))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Au tour du $activeLabel de bannir",
                        color = activeColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Zone centrale : grille 3×3 (3 lignes de catégories) ──
            listOf("Déploiement", "Objectif", "Condition").forEach { category ->
                val cards = battlefieldDeck.filter { it.category == category }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(category, color = Color(0xFF9EACBC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cards.forEach { card ->
                            val isBanned = card.id in banned
                            BattlefieldCardTile(
                                card = card,
                                banned = isBanned,
                                onClick = {
                                    // Toggle du véto : bannir/débannir, puis passer la main.
                                    banned = if (isBanned) banned - card.id else banned + card.id
                                    blueTurn = !blueTurn
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Footer : valider le champ de bataille ──
            Button(
                onClick = { onValidate(banned.toList()) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC857), contentColor = Color(0xFF0A0E15))
            ) {
                Text("Valider le champ de bataille", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ── Tuile de carte de bataille (avec état de véto) ─────────────────────────
@Composable
private fun BattlefieldCardTile(
    card: BattlefieldCard,
    banned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (banned) Color(0xFF1A1A22) else Color(0xFF192330),
        modifier = modifier
            .graphicsLayer {
                scaleX = if (banned) 0.92f else 1f
                scaleY = if (banned) 0.92f else 1f
            }
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.72f).padding(6.dp), contentAlignment = Alignment.Center) {
            // Grande icône "X" rouge par-dessus quand bannie.
            if (banned) {
                Text("✕", color = Color(0xFFEF4444), fontSize = 40.sp, fontWeight = FontWeight.Bold, modifier = Modifier.alpha(0.9f))
            }
            // Nom de la carte, toujours lisible en dessous.
            Text(
                card.name,
                color = if (banned) Color(0xFF6B7280) else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
            )
        }
    }
}
