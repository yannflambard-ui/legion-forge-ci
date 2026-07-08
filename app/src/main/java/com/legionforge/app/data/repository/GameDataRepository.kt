package com.legionforge.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.legionforge.app.data.local.LegionForgeDatabase
import com.legionforge.app.data.model.Faction
import com.legionforge.app.data.model.UnitEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository central pour les données du jeu (factions, unités).
 * Charge le catalogue depuis assets/game_data_core.json au premier lancement
 * puis sert tout depuis Room (offline-first).
 */
class GameDataRepository(private val context: Context) {

    private val db = LegionForgeDatabase.getInstance(context)
    private val dao = db.gameDataDao()

    suspend fun ensureSeeded() {
        if (dao.factionCount() > 0) return

        val json = context.assets.open("game_data_core.json").bufferedReader().use { it.readText() }
        val data = Gson().fromJson(json, GameDataFile::class.java)

        val factions = data.factions.map {
            Faction(
                id = it.id,
                name = it.name,
                description = it.description,
                imageUrl = it.imageUrl,
                expansionId = it.expansionId
            )
        }
        val units = data.units.map {
            UnitEntity(
                id = it.id,
                factionId = it.factionId,
                name = it.name,
                points = it.points,
                rank = it.rankEnum(),
                minInArmy = it.minInArmy,
                maxInArmy = it.maxInArmy,
                imageUrl = it.imageUrl,
                expansionId = it.expansionId,
                unique = it.unique
            )
        }

        dao.insertFactions(factions)
        dao.insertUnits(units)
    }

    fun getFactions(): Flow<List<Faction>> = dao.getAllFactions()

    fun getUnitsForFaction(factionId: String): Flow<List<UnitEntity>> =
        dao.getUnitsForFaction(factionId)
}
