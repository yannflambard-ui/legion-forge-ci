package com.legionforge.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.legionforge.app.data.local.LegionForgeDatabase
import com.legionforge.app.data.local.PolymorphicGameDao
import com.legionforge.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class BuilderRepository(context: Context) {
    private val dao: PolymorphicGameDao = LegionForgeDatabase.getInstance(context).polymorphicGameDao()
    private val gson = Gson()

    fun observeCards(system: GameSystem, factionId: String): Flow<List<CardDefinition>> =
        dao.observeCards(system.name, factionId).map { rows -> rows.map(CatalogCardEntity::toDefinition) }

    fun observeCards(system: GameSystem): Flow<List<CardDefinition>> =
        dao.observeCards(system.name).map { rows -> rows.map(CatalogCardEntity::toDefinition) }

    fun observeLists(): Flow<List<BuilderListEntity>> = dao.observeLists()

    suspend fun seedCatalog(context: Context) = withContext(Dispatchers.IO) {
        try {
            val existing = dao.cardCount()
            if (existing >= 400) return@withContext
            val document = context.assets.open("catalog.json").bufferedReader().use {
                gson.fromJson(it, CatalogDocument::class.java)
            } ?: return@withContext
            dao.upsertCards(document.cards.map(CatalogCardEntity::from))
        } catch (e: Exception) {
            android.util.Log.e("BuilderRepo", "Catalog seed failed", e)
        }
    }

    suspend fun createList(name: String, system: GameSystem, factionId: String, limit: Int): BuilderListEntity {
        val list = BuilderListEntity(UUID.randomUUID().toString(), name, system.name, factionId, limit, System.currentTimeMillis())
        dao.upsertList(list)
        return list
    }

    suspend fun saveList(list: BuilderListEntity, entries: List<ListEntry>) {
        dao.upsertList(list.copy(updatedAt = System.currentTimeMillis()))
        dao.replaceEntries(list.id, entries.map { entry ->
            BuilderEntryEntity(entry.instanceId, list.id, entry.card.id, entry.parentInstanceId, entry.quantity, entry.chosenSlot?.name)
        })
    }

    fun observeEntries(listId: String, cards: List<CardDefinition>): Flow<List<ListEntry>> {
        val byId = cards.associateBy(CardDefinition::id)
        return dao.observeEntries(listId).map { entities ->
            entities.mapNotNull { entity -> byId[entity.cardId]?.let { ListEntry(entity.instanceId, it, entity.parentInstanceId, entity.quantity, entity.chosenSlot?.let(ArmadaSlot::valueOf)) } }
        }
    }

    suspend fun getList(id: String) = dao.getList(id)
}
