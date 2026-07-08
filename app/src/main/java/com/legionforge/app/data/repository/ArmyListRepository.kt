package com.legionforge.app.data.repository

import android.content.Context
import com.legionforge.app.data.local.LegionForgeDatabase
import com.legionforge.app.data.model.ArmyList
import com.legionforge.app.data.model.ArmyUnit
import kotlinx.coroutines.flow.Flow

/** Repository pour les listes d'armée de l'utilisateur (CRUD local, offline-first). */
class ArmyListRepository(context: Context) {

    private val db = LegionForgeDatabase.getInstance(context)
    private val dao = db.armyListDao()

    fun getAllLists(): Flow<List<ArmyList>> = dao.getAllLists()

    fun getUnitsInList(armyListId: String): Flow<List<ArmyUnit>> = dao.getUnitsInList(armyListId)

    suspend fun getList(id: String): ArmyList? = dao.getList(id)

    suspend fun createList(list: ArmyList) = dao.upsertList(list)

    suspend fun updateList(list: ArmyList) = dao.upsertList(list.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteList(list: ArmyList) = dao.deleteList(list)

    suspend fun addUnit(unit: ArmyUnit): Long = dao.addUnitToList(unit)

    suspend fun removeUnit(unit: ArmyUnit) = dao.removeUnitFromList(unit)
}
