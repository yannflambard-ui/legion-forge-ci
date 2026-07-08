package com.legionforge.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.legionforge.app.data.model.ArmyList
import com.legionforge.app.data.model.ArmyUnit
import com.legionforge.app.data.model.UnitEntity
import com.legionforge.app.data.repository.ArmyListRepository
import com.legionforge.app.data.repository.GameDataRepository
import com.legionforge.app.domain.ArmyListValidator
import com.legionforge.app.domain.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** ViewModel du builder de liste : ajout/retrait d'unités et validation en direct. */
class ArmyBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val armyRepo = ArmyListRepository(application)
    private val gameRepo = GameDataRepository(application)

    private val _currentList = MutableStateFlow<ArmyList?>(null)
    val currentList: StateFlow<ArmyList?> = _currentList.asStateFlow()

    private val _availableUnits = MutableStateFlow<List<UnitEntity>>(emptyList())
    val availableUnits: StateFlow<List<UnitEntity>> = _availableUnits.asStateFlow()

    private val _listEntries = MutableStateFlow<List<Pair<UnitEntity, Int>>>(emptyList())
    val listEntries: StateFlow<List<Pair<UnitEntity, Int>>> = _listEntries.asStateFlow()

    private val _validation = MutableStateFlow(ValidationResult(true, 0, 1000, emptyList()))
    val validation: StateFlow<ValidationResult> = _validation.asStateFlow()

    fun startNewList(name: String, factionId: String) {
        viewModelScope.launch {
            val list = ArmyList(name = name, factionId = factionId)
            armyRepo.createList(list)
            _currentList.value = list
            _listEntries.value = emptyList()
            recomputeValidation()

            launch {
                gameRepo.getUnitsForFaction(factionId).collect { units ->
                    _availableUnits.value = units
                }
            }
        }
    }

    fun addUnit(unit: UnitEntity) {
        val current = _listEntries.value.toMutableList()
        val idx = current.indexOfFirst { it.first.id == unit.id }
        if (idx >= 0) {
            val (u, qty) = current[idx]
            current[idx] = u to (qty + 1)
        } else {
            current.add(unit to 1)
        }
        _listEntries.value = current
        recomputeValidation()
    }

    fun removeUnit(unit: UnitEntity) {
        val current = _listEntries.value.toMutableList()
        val idx = current.indexOfFirst { it.first.id == unit.id }
        if (idx >= 0) {
            val (u, qty) = current[idx]
            if (qty <= 1) {
                current.removeAt(idx)
            } else {
                current[idx] = u to (qty - 1)
            }
        }
        _listEntries.value = current
        recomputeValidation()
    }

    private fun recomputeValidation() {
        val limit = _currentList.value?.pointsLimit ?: 1000
        _validation.value = ArmyListValidator.validate(_listEntries.value, limit)
    }
}
