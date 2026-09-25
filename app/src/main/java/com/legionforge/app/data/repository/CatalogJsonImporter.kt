package com.legionforge.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.legionforge.app.data.local.PolymorphicGameDao
import com.legionforge.app.data.model.CatalogCardEntity
import com.legionforge.app.data.model.CatalogDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Charge un export JSON versionné hors-ligne (fichier assets ou contenu fourni par le pipeline). */
class CatalogJsonImporter(private val dao: PolymorphicGameDao, private val gson: Gson = Gson()) {
    suspend fun importJson(json: String) = withContext(Dispatchers.IO) {
        val document = gson.fromJson(json, CatalogDocument::class.java)
            ?: error("Catalogue JSON vide ou invalide")
        require(document.schemaVersion == 1) { "Version de schéma non supportée : ${document.schemaVersion}" }
        require(document.cards.map { it.id }.distinct().size == document.cards.size) { "IDs de cartes dupliqués" }
        require(document.cards.all { it.points >= 0 && it.name.isNotBlank() }) { "Carte invalide (nom vide ou points négatifs)" }
        dao.upsertCards(document.cards.map(CatalogCardEntity::from))
    }

    suspend fun importAsset(context: Context, assetName: String) = withContext(Dispatchers.IO) {
        importJson(context.assets.open(assetName).bufferedReader().use { it.readText() })
    }
}
