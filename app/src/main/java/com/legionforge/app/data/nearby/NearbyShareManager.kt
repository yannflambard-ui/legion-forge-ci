package com.legionforge.app.data.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** État global du partage Nearby Connections. */
enum class ShareMode { IDLE, ADVERTISING, DISCOVERING, CONNECTED }

data class DiscoveredEndpoint(val endpointId: String, val name: String)

/**
 * Gestionnaire Nearby Connections : partage live de listes entre 2 téléphones, sans serveur.
 * - Hôte : startAdvertising() -> attend qu'un client se connecte.
 * - Client : startDiscovery() -> voit les hôtes, connectTo(endpointId).
 * - Une fois connecté, sendList(json) envoie la liste ; l'autre la reçoit via receivedPayload.
 */
class NearbyShareManager(context: Context) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = "com.legionforge.app.share"

    private val _mode = MutableStateFlow(ShareMode.IDLE)
    val mode: StateFlow<ShareMode> = _mode.asStateFlow()

    private val _endpoints = MutableStateFlow<List<DiscoveredEndpoint>>(emptyList())
    val endpoints: StateFlow<List<DiscoveredEndpoint>> = _endpoints.asStateFlow()

    private val _connectedEndpoint = MutableStateFlow<String?>(null)
    val connectedEndpoint: StateFlow<String?> = _connectedEndpoint.asStateFlow()

    private val _receivedPayload = MutableStateFlow<String?>(null)
    val receivedPayload: StateFlow<String?> = _receivedPayload.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Accepter automatiquement la connexion (pas de code d'appairage).
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    _connectedEndpoint.value = endpointId
                    _mode.value = ShareMode.CONNECTED
                }
                else -> _error.value = "Connexion refusée (${result.status.statusCode})"
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (_connectedEndpoint.value == endpointId) {
                _connectedEndpoint.value = null
                _mode.value = ShareMode.IDLE
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            _receivedPayload.value = String(bytes, Charsets.UTF_8)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Progression d'envoi/réception (ignorée pour l'instant).
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            _endpoints.value = _endpoints.value + DiscoveredEndpoint(endpointId, info.endpointName)
        }

        override fun onEndpointLost(endpointId: String) {
            _endpoints.value = _endpoints.value.filterNot { it.endpointId == endpointId }
        }
    }

    /** Hôte : diffuse sa présence et attend un client. */
    fun startAdvertising() {
        stop()
        _endpoints.value = emptyList()
        _error.value = null
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startAdvertising(
            android.os.Build.MODEL, serviceId, lifecycleCallback, options
        ).addOnSuccessListener { _mode.value = ShareMode.ADVERTISING }
            .addOnFailureListener { e -> _error.value = "Erreur hôte: ${e.message}" }
    }

    /** Client : cherche les hôtes à proximité. */
    fun startDiscovery() {
        stop()
        _endpoints.value = emptyList()
        _error.value = null
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, options)
            .addOnSuccessListener { _mode.value = ShareMode.DISCOVERING }
            .addOnFailureListener { e -> _error.value = "Erreur client: ${e.message}" }
    }

    /** Client : se connecte à un hôte découvert. */
    fun connectTo(endpointId: String) {
        connectionsClient.requestConnection(android.os.Build.MODEL, endpointId, lifecycleCallback)
            .addOnFailureListener { e -> _error.value = "Connexion impossible: ${e.message}" }
    }

    /** Envoie la liste (JSON) à l'endpoint connecté. */
    fun sendList(json: String) {
        val endpoint = _connectedEndpoint.value ?: return
        connectionsClient.sendPayload(endpoint, Payload.fromBytes(json.toByteArray(Charsets.UTF_8)))
    }

    fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _mode.value = ShareMode.IDLE
        _endpoints.value = emptyList()
        _connectedEndpoint.value = null
    }
}
