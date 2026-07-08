package com.legionforge.app.data.repository

import com.legionforge.app.data.model.UnitRank

/** DTOs miroir du fichier JSON pour le parsing Gson, découplés des entités Room. */
data class GameDataFile(
    val version: String,
    val gameVersion: String,
    val updated: String,
    val factions: List<FactionDto>,
    val units: List<UnitDto>
)

data class FactionDto(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val expansionId: String? = null
)

data class UnitDto(
    val id: String,
    val factionId: String,
    val name: String,
    val points: Int,
    val rank: String,
    val minInArmy: Int,
    val maxInArmy: Int,
    val imageUrl: String? = null,
    val expansionId: String? = null,
    val unique: Boolean = false
) {
    fun rankEnum(): UnitRank = UnitRank.valueOf(rank)
}
