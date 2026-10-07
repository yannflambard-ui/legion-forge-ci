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
import androidx.core.content.ContextCompat

/**
 * Permissions runtime requises par Nearby Connections (mode 2 joueurs + partage live).
 * - ACCESS_FINE_LOCATION : API <= 30 (Bluetooth/WiFi discovery)
 * - BLUETOOTH_SCAN + BLUETOOTH_CONNECT : API 31+
 * - NEARBY_WIFI_DEVICES : API 33+
 */
fun nearbyPermissions(): Array<String> = buildList {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.NEARBY_WIFI_DEVICES)
    }
}.toTypedArray()

fun Context.nearbyPermissionsGranted(): Boolean =
    nearbyPermissions().all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

/**
 * Renvoie une fonction [runWithPermission] à appeler AVANT toute action Nearby
 * (démarrer l'hôte, démarrer le client, se connecter à un endpoint).
 * Si les permissions manquent, les demande au runtime puis exécute l'action une fois accordées.
 *
 * Usage :
 *   val runWithPermission = rememberNearbyPermissionAction()
 *   Button(onClick = { runWithPermission { vm.startHost() } }) { ... }
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
