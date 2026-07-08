package com.legionforge.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.legionforge.app.data.model.Faction
import com.legionforge.app.data.model.UnitEntity
import com.legionforge.app.data.repository.GameDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameDataViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameDataRepository(application)

    private val _factions = MutableStateFlow<List<Faction>>(emptyList())
    val factions: StateFlow<List<Faction>> = _factions.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            _isReady.value = true
            repository.getFactions().collect { _factions.value = it }
        }
    }

    fun unitsForFaction(factionId: String) = repository.getUnitsForFaction(factionId)
}
