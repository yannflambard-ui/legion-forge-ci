package com.legionforge.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel

/** Écran principal du builder : liste des unités disponibles + liste en cours + validation live. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmyBuilderScreen(
    factionId: String,
    listName: String,
    viewModel: ArmyBuilderViewModel = viewModel()
) {
    LaunchedEffect(factionId) {
        viewModel.startNewList(listName, factionId)
    }

    val availableUnits by viewModel.availableUnits.collectAsState()
    val entries by viewModel.listEntries.collectAsState()
    val validation by viewModel.validation.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(listName) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${validation.totalPoints} / ${validation.pointsLimit} pts",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (validation.totalPoints > validation.pointsLimit) Color.Red else MaterialTheme.colorScheme.primary
                )
            }

            if (validation.errors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                validation.errors.forEach { error ->
                    Text(text = "⚠ $error", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "✓ Liste valide", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(text = "Dans la liste (${entries.size})", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.height(180.dp)) {
                items(entries) { (unit, qty) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "${unit.name} x$qty — ${unit.points * qty} pts")
                        Row {
                            OutlinedButton(onClick = { viewModel.removeUnit(unit) }) { Text("-") }
                            Spacer(modifier = Modifier.height(0.dp))
                            Button(onClick = { viewModel.addUnit(unit) }) { Text("+") }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            Text(text = "Catalogue", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(availableUnits) { unit ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = unit.name, style = MaterialTheme.typography.bodyLarge)
                                Text(text = "${unit.rank} — ${unit.points} pts", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { viewModel.addUnit(unit) }) { Text("Ajouter") }
                        }
                    }
                }
            }
        }
    }
}
