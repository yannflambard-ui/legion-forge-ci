package com.legionforge.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.legionforge.app.data.model.BuilderEntryEntity
import com.legionforge.app.data.model.BuilderListEntity
import com.legionforge.app.data.model.CatalogCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PolymorphicGameDao {
    @Query("SELECT * FROM catalog_cards WHERE gameSystem = :system AND (factionId = :factionId OR factionId = 'neutral') ORDER BY name")
    fun observeCards(system: String, factionId: String): Flow<List<CatalogCardEntity>>

    @Query("SELECT * FROM catalog_cards WHERE gameSystem = :system ORDER BY factionId, name")
    fun observeCards(system: String): Flow<List<CatalogCardEntity>>

    @Query("SELECT COUNT(*) FROM catalog_cards")
    suspend fun cardCount(): Int

    @Query("SELECT gameSystem, COUNT(*) AS cnt FROM catalog_cards GROUP BY gameSystem")
    suspend fun cardCountBySystem(): List<SystemCountRow>

    data class SystemCountRow(val gameSystem: String, val cnt: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCards(cards: List<CatalogCardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertList(list: BuilderListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<BuilderEntryEntity>)

    @Query("DELETE FROM builder_entries WHERE listId = :listId")
    suspend fun deleteEntries(listId: String)

    @Query("SELECT * FROM builder_lists ORDER BY updatedAt DESC")
    fun observeLists(): Flow<List<BuilderListEntity>>

    @Query("SELECT * FROM builder_lists WHERE id = :listId LIMIT 1")
    suspend fun getList(listId: String): BuilderListEntity?

    @Query("SELECT * FROM builder_entries WHERE listId = :listId ORDER BY rowid")
    fun observeEntries(listId: String): Flow<List<BuilderEntryEntity>>

    @Transaction
    suspend fun replaceEntries(listId: String, entries: List<BuilderEntryEntity>) {
        deleteEntries(listId)
        if (entries.isNotEmpty()) upsertEntries(entries)
    }
}
