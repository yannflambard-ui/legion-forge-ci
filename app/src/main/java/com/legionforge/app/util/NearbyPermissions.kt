package com.legionforge.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * Permissions runtime requises par Nearby Connections (mode 2 joueurs + partage live).
 *
 * Le Nearby a DEUX rôles distincts → deux souches de permission :
 *  - HÔTE = advertising Nearby : réclame BLUETOOTH_ADVERTISE (API 31+).
 *  - CLIENT = discovery Nearby : réclame le scan (API 31+ ) / ACCURACY si < 31.
 *
 * Symptômes codes :
 *  - MISSING_PERMISSION_BLUETOOTH_ADVERTISE (hôte) → BLUETOOTH_ADVERTISE manquant.
 *  - MISSING_PERMISSION_ACCESS_COARSE_LOCATION (client, pré-31) → ACCURACY.
 */
fun nearbyPermissions(): Array<String> = buildList {
    // Nearby Connections exige ACCESS_COARSE_LOCATION au runtime sur TOUTES les versions
    // Android (le check Nearby le réclame même en 13+, indépendamment de BLUETOOTH_SCAN).
    // Sans lui -> erreur client "8034: MISSING_PERMISSION_ACCESS_COARSE_LOCATION".
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_ADVERTISE)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    // Api 33+ : Nearby passe par le Wi-Fi P2P → NEARBY_WIFI_DEVICES (runtime).
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }
}.toTypedArray()

fun nearbyPermissionLabels(): List<String> = nearbyPermissions().map { p ->
    when (p) {
        Manifest.permission.ACCESS_COARSE_LOCATION -> "Localisation approximative"
        Manifest.permission.ACCESS_FINE_LOCATION -> "Localisation précise"
        Manifest.permission.BLUETOOTH_SCAN -> "Bluetooth : scan des appareils"
        Manifest.permission.BLUETOOTH_ADVERTISE -> "Bluetooth : publicité Nearby (hôte)"
        Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth : connexion"
        Manifest.permission.NEARBY_WIFI_DEVICES -> "Wi-Fi : appareils à proximité"
        else -> p
    }
}

// Libellés intrusifs pour la demande de permission 2 joueurs.
fun nearbyPermissionRationales(): List<String> = nearbyPermissionLabels()

fun Context.nearbyPermissionsGranted(): Boolean =
    nearbyPermissions().all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

// ── Panneau de diagnostic Nearby (mode 2 joueurs) ─────────────────────────
// Affiche l'état réel de chaque permission (✓/✗), l'erreur Nearby traduite, et un
// bouton « Signaler » qui pousse l'état + l'erreur sur GitHub (issue) pour qu'on
// puisse comprendre ce qui se passe à distance.
@Composable
fun NearbyDiagnosticPanel(
    error: String?,
    onReport: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val perms = remember { nearbyPermissions() }
    val labels = remember { nearbyPermissionLabels() }
    val granted = remember(perms) { perms.map { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED } }
    val missing = granted.count { !it }
    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (missing == 0) "✓ Permissions Nearby : toutes accordées."
            else "✗ ${missing} permission(s) Nearby manquante(s) — c'est la cause probable.",
            color = if (missing == 0) Color(0xFF77D9A7) else Color(0xFFFF6B6B),
            fontSize = 11.sp, fontWeight = FontWeight.Bold
        )
        perms.forEachIndexed { i, p ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (granted[i]) "✓" else "✗", color = if (granted[i]) Color(0xFF77D9A7) else Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text(labels[i], color = if (granted[i]) Color(0xFFB4C2D4) else Color.White, fontSize = 11.sp, fontWeight = if (granted[i]) FontWeight.Normal else FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(p.substringAfterLast('.'), color = Color(0xFF7A8A9A), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (error != null) {
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF3B2224), modifier = Modifier.fillMaxWidth()) {
                Text(error, Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = Color(0xFFFFC7B7), fontSize = 11.sp)
            }
        }
        TextButton(onClick = {
            val state = perms.mapIndexed { i, p -> "${if (granted[i]) "OK" else "MANQUANT"} $p" }.joinToString("\n")
            onReport("Nearby 2 joueurs — ${missing} permission(s) manquante(s)\n$state\nErreur: ${error ?: "aucune"}")
        }) { Text("Signaler sur GitHub", color = Color(0xFFFFC857), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    }
}

/**
 * Affiche un bouton « OK » dans la demande Nearby runtime, puis exécute l'action
 * une fois les permissions accordées. Retourne une fonction qui, appelée, lance
 * la demande et exécute [action] au callback.
 */
@Composable
fun rememberNearbyPermissionAction(): (() -> Unit) -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pending = remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (context.nearbyPermissionsGranted()) pending.value?.invoke()
        pending.value = null
    }
    return { action ->
        if (context.nearbyPermissionsGranted()) {
            action()
        } else {
            pending.value = action
            launcher.launch(nearbyPermissions())
        }
    }
}
