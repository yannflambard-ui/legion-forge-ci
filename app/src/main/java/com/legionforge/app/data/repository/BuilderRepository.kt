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
            android.util.Log.i("Repo", "seedCatalog: checking counts by system")
            val counts = dao.cardCountBySystem().associate { it.gameSystem to it.cnt }
            val legionCount = counts[GameSystem.LEGION_V2.name] ?: 0
            val armadaCount = counts[GameSystem.ARMADA_V15.name] ?: 0
            android.util.Log.i("Repo", "seedCatalog: LEGION=$legionCount, ARMADA=$armadaCount")
            if (legionCount >= 190 && armadaCount >= 40) {
                android.util.Log.i("Repo", "seedCatalog: both systems populated, skipping")
                return@withContext
            }
            android.util.Log.i("Repo", "seedCatalog: stale/empty, forcing full reseed (legion=$legionCount, armada=$armadaCount)")
            val json = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
            android.util.Log.i("Repo", "seedCatalog: read ${json.length} chars, parsing with Gson")
            val document = gson.fromJson(json, CatalogDocument::class.java)
            if (document == null) {
                android.util.Log.w("Repo", "seedCatalog: Gson returned null")
                return@withContext
            }
            android.util.Log.i("Repo", "seedCatalog: parsed ${document.cards.size} cards")
            val entities = document.cards.mapNotNull { card ->
                try {
                    CatalogCardEntity.from(card)
                } catch (e: Exception) {
                    android.util.Log.e("Repo", "seedCatalog: failed to map card ${card.id}", e); null
                }
            }
            android.util.Log.i("Repo", "seedCatalog: mapped ${entities.size}/${document.cards.size} entities")
            entities.chunked(200).forEachIndexed { i, chunk ->
                android.util.Log.i("Repo", "seedCatalog: inserting chunk $i (${chunk.size})")
                dao.upsertCards(chunk)
            }
            val after = dao.cardCountBySystem().joinToString(", ") { "${it.gameSystem}=${it.cnt}" }
            android.util.Log.i("Repo", "seedCatalog: done - counts now {$after}")
        } catch (e: Exception) {
            android.util.Log.e("Repo", "seedCatalog FAILED", e)
            // Rethrow pour que le ViewModel puisse afficher l'erreur au lieu d'un écran vide.
            throw e
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

    suspend fun catalogDiagnostics(): String {
        val counts = dao.cardCountBySystem().joinToString(", ") { "${it.gameSystem}=${it.cnt}" }
        val total = dao.cardCount()
        return "total=$total; countsBySystem={$counts}; db=${android.os.Build.MODEL} SDK=${android.os.Build.VERSION.SDK_INT}"
    }
}
