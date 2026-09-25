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

    // Mémorise le dernier état du seed pour le diagnostic remote.
    @Volatile private var lastSeed: String = "seed jamais exécuté"

    suspend fun seedCatalog(context: Context) = withContext(Dispatchers.IO) {
        try {
            lastSeed = "seedCatalog: vérifie les comptes"
            val counts = dao.cardCountBySystem().associate { it.gameSystem to it.cnt }
            val legionCount = counts[GameSystem.LEGION_V2.name] ?: 0
            val armadaCount = counts[GameSystem.ARMADA_V15.name] ?: 0
            android.util.Log.i("Repo", "seedCatalog: LEGION=$legionCount, ARMADA=$armadaCount")
            if (legionCount >= 190 && armadaCount >= 40) {
                lastSeed = "skip: déjà peuplé (LEGION=$legionCount, ARMADA=$armadaCount)"
                return@withContext
            }
            lastSeed = "reseed forcé (LEGION=$legionCount, ARMADA=$armadaCount)"
            val json = try { context.assets.open("catalog.json").bufferedReader().use { it.readText() } }
            catch (e: Exception) { lastSeed = "ERREUR lecture asset: ${e.message}"; throw e }
            lastSeed = "asset lu: ${json.length} chars"
            val document = try { gson.fromJson(json, CatalogDocument::class.java) }
            catch (e: Exception) { lastSeed = "ERREUR Gson parse: ${e.message}"; throw e }
            if (document == null) {
                lastSeed = "ERREUR: Gson a retourné null (${json.length} chars)"
                return@withContext
            }
            lastSeed = "parsed ${document.cards.size} cartes"
            val entities = document.cards.mapNotNull { card ->
                try {
                    CatalogCardEntity.from(card)
                } catch (e: Exception) {
                    android.util.Log.e("Repo", "seedCatalog: failed to map card ${card.id}", e)
                    lastSeed = "ERREUR mapping carte ${card.id}: ${e.message}"
                    null
                }
            }
            lastSeed = "mappées ${entities.size}/${document.cards.size} entités"
            entities.chunked(200).forEachIndexed { i, chunk ->
                try {
                    dao.upsertCards(chunk)
                } catch (e: Exception) {
                    lastSeed = "ERREUR insert chunk $i: ${e.message}"
                    throw e
                }
            }
            val after = dao.cardCountBySystem().joinToString(", ") { "${it.gameSystem}=${it.cnt}" }
            lastSeed = "terminé: counts {$after}"
            android.util.Log.i("Repo", "seedCatalog: done - $after")
        } catch (e: Exception) {
            android.util.Log.e("Repo", "seedCatalog FAILED", e)
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
        return "total=$total; countsBySystem={$counts}; seed=$lastSeed; db=${android.os.Build.MODEL} SDK=${android.os.Build.VERSION.SDK_INT}"
    }
}
