package com.legionforge.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.legionforge.app.data.model.*
import com.legionforge.app.data.repository.BuilderRepository
import com.legionforge.app.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class ArmyBuilderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BuilderRepository(application)
    private val _cards = MutableStateFlow<List<CardDefinition>>(emptyList())
    val cards: StateFlow<List<CardDefinition>> = _cards.asStateFlow()
    private val _allLists = MutableStateFlow<List<BuilderListEntity>>(emptyList())
    val allLists: StateFlow<List<BuilderListEntity>> = _allLists.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _catalogError = MutableStateFlow<String?>(null)
    val catalogError: StateFlow<String?> = _catalogError.asStateFlow()
    private val _currentList = MutableStateFlow<BuilderListEntity?>(null)
    val currentList: StateFlow<BuilderListEntity?> = _currentList.asStateFlow()
    private val _entries = MutableStateFlow<List<ListEntry>>(emptyList())
    val entries: StateFlow<List<ListEntry>> = _entries.asStateFlow()
    private val _validation = MutableStateFlow(RuleValidationResult(false, 0, emptyList()))
    val validation: StateFlow<RuleValidationResult> = _validation.asStateFlow()
    private var entryCollector: kotlinx.coroutines.Job? = null
    private val saveMutex = kotlinx.coroutines.sync.Mutex()
    private var seedJob: kotlinx.coroutines.Job? = null

    init {
        // Force loading to end after 30s even if seed hangs
        viewModelScope.launch {
            kotlinx.coroutines.delay(30_000)
            if (_loading.value) {
                android.util.Log.w("VM", "Catalog seed timed out after 30s - forcing loading=false")
                _loading.value = false
                _catalogError.value = "Chargement du catalogue interrompu (timeout)"
            }
        }
        seedJob = viewModelScope.launch {
            try {
                android.util.Log.i("VM", "Starting catalog seed")
                repository.seedCatalog(getApplication())
                android.util.Log.i("VM", "Catalog seed complete")
            } catch (e: Exception) {
                android.util.Log.e("VM", "Catalog seed crashed", e)
                _catalogError.value = "Erreur: ${e.message?.take(80) ?: e.javaClass.simpleName}"
            } finally {
                _loading.value = false
            }
        }
        viewModelScope.launch { repository.observeLists().collect { _allLists.value = it } }
    }

    private suspend fun awaitCatalog() {
        seedJob?.let {
            kotlinx.coroutines.withTimeoutOrNull(10_000) { it.join() }
        }
    }

    private var catalogCollector: kotlinx.coroutines.Job? = null

    fun loadCatalog(system: GameSystem, factionId: String) {
        catalogCollector?.cancel()
        catalogCollector = viewModelScope.launch {
            awaitCatalog()
            repository.observeCards(system, factionId).collect { cards ->
                _cards.value = cards
                recalculate()
                _currentList.value?.takeIf { it.factionId == factionId }?.let(::watchEntries)
            }
        }
    }

    fun loadAllCatalog(system: GameSystem) {
        catalogCollector?.cancel()
        catalogCollector = viewModelScope.launch {
            awaitCatalog()
            repository.observeCards(system).collect { cards -> _cards.value = cards }
        }
    }

    fun createList(name: String, system: GameSystem, factionId: String, limit: Int, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            awaitCatalog()
            val list = repository.createList(name, system, factionId, limit)
            _currentList.value = list
            _entries.value = emptyList()
            loadCatalog(system, factionId)
            persistAndValidate()
            onCreated(list.id)
        }
    }

    fun openList(id: String) {
        viewModelScope.launch {
            awaitCatalog()
            val list = repository.getList(id) ?: return@launch
            _currentList.value = list
            val system = GameSystem.valueOf(list.gameSystem)
            catalogCollector?.cancel()
            _cards.value = repository.observeCards(system, list.factionId).first()
            watchEntries(list)
        }
    }

    private fun watchEntries(list: BuilderListEntity) {
        entryCollector?.cancel()
        entryCollector = viewModelScope.launch {
            repository.observeEntries(list.id, _cards.value).collect { loaded ->
                _entries.value = loaded
                recalculate()
            }
        }
    }

    fun add(card: CardDefinition, parentId: String? = null, chosenSlot: ArmadaSlot? = null) {
        _entries.value = _entries.value + ListEntry(UUID.randomUUID().toString(), card, parentId, 1, chosenSlot)
        persistAndValidate()
    }

    fun remove(entry: ListEntry) {
        val removedIds = (_entries.value.filter { it.instanceId == entry.instanceId || it.parentInstanceId == entry.instanceId }).map { it.instanceId }.toSet()
        _entries.value = _entries.value.filterNot { it.instanceId in removedIds }
        persistAndValidate()
    }

    private fun persistAndValidate() {
        val list = _currentList.value ?: return
        val updated = list.copy(updatedAt = System.currentTimeMillis())
        _currentList.value = updated
        val snapshot = _entries.value.toList()
        viewModelScope.launch { saveMutex.withLock { repository.saveList(updated, snapshot) } }
        recalculate()
    }

    private fun recalculate() {
        val list = _currentList.value ?: return
        val system = GameSystem.valueOf(list.gameSystem)
        val builder = BuilderList(list.id, list.name, system, list.factionId, list.pointsLimit, _entries.value)
        _validation.value = if (system == GameSystem.LEGION_V2) LegionV2Validator().validate(builder) else ArmadaV15Validator().validate(builder)
    }

    fun cardById(id: String) = _cards.value.firstOrNull { it.id == id }
}
