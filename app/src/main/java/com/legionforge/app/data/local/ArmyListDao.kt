package com.legionforge.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.legionforge.app.data.model.ArmyList
import com.legionforge.app.data.model.ArmyUnit
import kotlinx.coroutines.flow.Flow

@Dao
interface ArmyListDao {
    @Query("SELECT * FROM army_lists ORDER BY updatedAt DESC")
    fun getAllLists(): Flow<List<ArmyList>>

    @Query("SELECT * FROM army_lists WHERE id = :id LIMIT 1")
    suspend fun getList(id: String): ArmyList?

    @Query("SELECT * FROM army_units WHERE armyListId = :armyListId")
    fun getUnitsInList(armyListId: String): Flow<List<ArmyUnit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(list: ArmyList)

    @Insert
    suspend fun addUnitToList(unit: ArmyUnit): Long

    @Update
    suspend fun updateUnitInList(unit: ArmyUnit)

    @Delete
    suspend fun removeUnitFromList(unit: ArmyUnit)

    @Delete
    suspend fun deleteList(list: ArmyList)
}
