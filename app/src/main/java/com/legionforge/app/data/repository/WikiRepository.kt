package com.legionforge.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.legionforge.app.data.local.LegionForgeDatabase
import com.legionforge.app.data.local.WikiDao
import com.legionforge.app.data.model.WikiDocument
import com.legionforge.app.data.model.WikiSectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Wiki des règles officielles (Armada + Legion) : seed depuis assets/wiki.json
 *  puis recherche plein texte par mot-clé. Offline-first comme le catalogue. */
class WikiRepository(context: Context) {
    private val dao: WikiDao = LegionForgeDatabase.getInstance(context).wikiDao()
    private val gson = Gson()

    suspend fun seedWiki(context: Context) = withContext(Dispatchers.IO) {
        if (dao.sectionCount() > 0) return@withContext
        val json = context.assets.open("wiki.json").bufferedReader().use { it.readText() }
        val doc = gson.fromJson(json, WikiDocument::class.java)
            ?: error("Wiki JSON vide ou invalide")
        require(doc.schemaVersion == 1) { "Version de schéma wiki non supportée : ${doc.schemaVersion}" }
        val entities = doc.sections.map { s ->
            WikiSectionEntity(
                id = s.id,
                gameSystem = s.gameSystem,
                title = s.title,
                keywords = s.keywords.joinToString(","),
                content = s.content
            )
        }
        dao.upsertSections(entities)
    }

    fun observeSections(system: com.legionforge.app.data.model.GameSystem): Flow<List<WikiSectionEntity>> =
        dao.observeSections(system.name)

    fun observeAllSections(): Flow<List<WikiSectionEntity>> = dao.observeAllSections()

    fun searchSections(q: String): Flow<List<WikiSectionEntity>> = dao.searchSections(q)

    suspend fun getSection(id: String) = dao.getSection(id)
}
