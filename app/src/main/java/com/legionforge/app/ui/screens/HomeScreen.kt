package com.legionforge.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legionforge.app.ui.viewmodel.GameDataViewModel

/** Écran d'accueil : liste des listes d'armée existantes + bouton nouvelle liste. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewList: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Legion Forge") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Mes listes",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucune liste pour le moment. Crée ta première armée !",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNewList) {
                Text("Nouvelle liste")
            }
        }
    }
}

/** Écran de sélection de faction avant de créer une liste. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FactionPickerScreen(
    onFactionSelected: (factionId: String) -> Unit,
    viewModel: GameDataViewModel = viewModel()
) {
    val factions by viewModel.factions.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Choisir une faction") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            items(factions) { faction ->
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = faction.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = faction.description, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onFactionSelected(faction.id) }) {
                            Text("Sélectionner")
                        }
                    }
                }
            }
        }
    }
}
