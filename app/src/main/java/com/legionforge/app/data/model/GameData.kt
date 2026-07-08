package com.legionforge.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Une faction jouable (Rebel, Empire, Republic, Separatist, Mercenary...). */
@Entity(tableName = "factions")
data class Faction(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    /** null = disponible gratuitement (core box) */
    val expansionId: String? = null
)

/** Rang d'une unité dans une liste d'armée. */
enum class UnitRank {
    COMMANDER,
    OPERATIVE,
    CORPS,
    SPECIAL_FORCES,
    SUPPORT,
    HEAVY
}

/** Une unité du catalogue (pas une instance dans une liste). */
@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey val id: String,
    val factionId: String,
    val name: String,
    val points: Int,
    val rank: UnitRank,
    val minInArmy: Int,
    val maxInArmy: Int,
    val imageUrl: String? = null,
    /** null = unité gratuite (core box) */
    val expansionId: String? = null,
    val unique: Boolean = false
)

/** Un slot d'amélioration disponible sur une unité (ex: "Personnel", "Heavy Weapon"). */
@Entity(tableName = "upgrade_slots")
data class UpgradeSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: String,
    val slotType: String
)

/** Un mot-clé/keyword de règle (ex: "Django", "Ambush", "Scout").*/
@Entity(tableName = "keywords")
data class Keyword(
    @PrimaryKey val id: String,
    val name: String,
    val description: String
)

/** Association unité <-> keyword (many-to-many via table simple). */
@Entity(tableName = "unit_keywords", primaryKeys = ["unitId", "keywordId"])
data class UnitKeywordCrossRef(
    val unitId: String,
    val keywordId: String
)
