package com.legionforge.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.legionforge.app.data.model.Faction
import com.legionforge.app.data.model.Keyword
import com.legionforge.app.data.model.UnitEntity
import com.legionforge.app.data.model.UnitKeywordCrossRef
import com.legionforge.app.data.model.UpgradeSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDataDao {
    @Query("SELECT * FROM factions")
    fun getAllFactions(): Flow<List<Faction>>

    @Query("SELECT * FROM units WHERE factionId = :factionId")
    fun getUnitsForFaction(factionId: String): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units WHERE id = :unitId LIMIT 1")
    suspend fun getUnit(unitId: String): UnitEntity?

    @Query("SELECT * FROM upgrade_slots WHERE unitId = :unitId")
    suspend fun getUpgradeSlots(unitId: String): List<UpgradeSlot>

    @Query("SELECT k.* FROM keywords k INNER JOIN unit_keywords uk ON k.id = uk.keywordId WHERE uk.unitId = :unitId")
    suspend fun getKeywordsForUnit(unitId: String): List<Keyword>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFactions(factions: List<Faction>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeywords(keywords: List<Keyword>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpgradeSlots(slots: List<UpgradeSlot>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnitKeywords(crossRefs: List<UnitKeywordCrossRef>)

    @Query("SELECT COUNT(*) FROM factions")
    suspend fun factionCount(): Int
}
