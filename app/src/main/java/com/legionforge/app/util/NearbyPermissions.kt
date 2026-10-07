package com.legionforge.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
    // Legacy Nearby pre-Android 12 : le scan Bluetooth passait par la localisation.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
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
