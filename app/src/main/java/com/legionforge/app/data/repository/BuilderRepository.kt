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

    // Mémoise la trace complète du seed pour le diagnostic remote (chaque étape s'ajoute).
    @Volatile private var seedLog: String = "seed jamais exécuté"

    suspend fun seedCatalog(context: Context) = withContext(Dispatchers.IO) {
        try {
            var probes = StringBuilder()
            fun logProbe(name: String, value: Any?): Unit {
                probes.append(name).append("=").append(value).append("; ")
                seedLog = "seed: " + probes.toString()
            }
            logProbe("debut", "verifComptes")
            val counts = dao.cardCountBySystem().associate { it.gameSystem to it.cnt }
            val legionCount = counts[GameSystem.LEGION_V2.name] ?: 0
            val armadaCount = counts[GameSystem.ARMADA_V15.name] ?: 0
            android.util.Log.i("Repo", "seedCatalog: LEGION=$legionCount, ARMADA=$armadaCount")
            if (legionCount >= 190 && armadaCount >= 40) {
                seedLog = "skip: déjà peuplé (LEGION=$legionCount, ARMADA=$armadaCount)"
                return@withContext
            }
            logProbe("reseed", "forge (L=$legionCount A=$armadaCount)")
            val json = try { context.assets.open("catalog.json").bufferedReader().use { it.readText() } }
            catch (e: Exception) { seedLog = "ERREUR lecture asset: ${e.message}"; throw e }
            logProbe("assetChars", json.length)
            val document = try { gson.fromJson(json, CatalogDocument::class.java) }
            catch (e: Exception) { seedLog = "ERREUR Gson parse: ${e.message}"; throw e }
            if (document == null) { seedLog = "ERREUR: Gson a retourné null (${json.length} chars)"; return@withContext }
            logProbe("cartesParsees", document.cards.size)
            val entities = document.cards.mapNotNull { card ->
                try { CatalogCardEntity.from(card) }
                catch (e: Exception) {
                    android.util.Log.e("Repo", "seedCatalog: failed to map card ${card.id}", e)
                    seedLog = "ERREUR mapping carte ${card.id}: ${e.message}"; null
                }
            }
            logProbe("entitesMappees", entities.size)
            entities.chunked(200).forEachIndexed { i, chunk ->
                try {
                    val inserted = dao.upsertCardsCounted(chunk)
                    logProbe("chunk${i}Insert", inserted)
                    logProbe("countApresChunk${i}", dao.cardCountBySystem().joinToString(",") { "${it.gameSystem}=${it.cnt}" })
                } catch (e: Exception) {
                    seedLog = "ERREUR insert chunk $i: ${e.message}"; throw e
                }
            }
            val after = dao.cardCountBySystem().joinToString(", ") { "${it.gameSystem}=${it.cnt}" }
            logProbe("totalApresSeed", dao.cardCount())
            logProbe("countsApresSeed", "{${after}}")
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
        return "total=$total; countsBySystem={$counts}; seed=${seedLog.replace("\n", " | ")}; db=${android.os.Build.MODEL} SDK=${android.os.Build.VERSION.SDK_INT}"
    }
}
