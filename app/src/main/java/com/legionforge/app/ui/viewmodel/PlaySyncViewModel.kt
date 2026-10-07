package com.legionforge.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.legionforge.app.data.model.BuilderListEntity
import com.legionforge.app.data.model.ListEntry
import com.legionforge.app.data.nearby.PlaySyncManager
import com.legionforge.app.data.nearby.ShareMode
import com.legionforge.app.data.nearby.SyncedUnitState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ViewModel du mode play 2 joueurs : sync live via Nearby. */
class PlaySyncViewModel(application: Application) : AndroidViewModel(application) {
    private val gson = Gson()
    private val sync = PlaySyncManager(application)

    val mode: StateFlow<ShareMode> = sync.mode
    val endpoints: StateFlow<List<com.legionforge.app.data.nearby.DiscoveredEndpoint>> = sync.endpoints
    val connectedEndpoint: StateFlow<String?> = sync.connectedEndpoint
    val error: StateFlow<String?> = sync.error
    val receivedRound: StateFlow<Int?> = sync.receivedRound
    val receivedUnits: StateFlow<List<SyncedUnitState>> = sync.receivedUnits

    private val _opponentListJson = MutableStateFlow<String?>(null)
    val opponentListJson: StateFlow<String?> = _opponentListJson.asStateFlow()

    // Cache de la dernière liste envoyée, pour pouvoir la renvoyer sur demande.
    private var lastList: BuilderListEntity? = null
    private var lastEntries: List<ListEntry> = emptyList()

    init {
        viewModelScope.launch {
            sync.receivedListJson.collect { json -> _opponentListJson.value = json }
        }
        // Quand l'autre joueur demande la synchro, on lui renvoie notre liste.
        viewModelScope.launch {
            sync.listRequested.collect { requested ->
                if (requested) {
                    val l = lastList
                    if (l != null) sendList(l, lastEntries)
                }
            }
        }
    }

    fun startHost() = sync.startHost()
    fun startClient() = sync.startClient()
    fun connectTo(endpointId: String) = sync.connectTo(endpointId)
    fun stop() = sync.stop()

    fun sendRound(round: Int) = sync.sendRound(round)
    fun sendState(units: List<SyncedUnitState>) = sync.sendState(units)

    /** Demande à l'autre joueur de renvoyer sa liste (synchro manuelle). */
    fun requestList() = sync.requestList()

    /** Sérialise la liste courante et l'envoie à l'autre téléphone. */
    fun sendList(list: BuilderListEntity?, entries: List<ListEntry>) {
        val l = list ?: return
        lastList = l
        lastEntries = entries
        val payload = ShareListPayload(
            name = l.name, gameSystem = l.gameSystem, factionId = l.factionId,
            pointsLimit = l.pointsLimit,
            entries = entries.map { e -> ShareEntry(e.card.id, e.parentInstanceId, e.quantity, e.chosenSlot?.name) }
        )
        sync.sendList(gson.toJson(payload))
    }

    override fun onCleared() {
        sync.stop()
        super.onCleared()
    }
}
