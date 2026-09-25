package com.legionforge.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Système identifié dans les catalogues JSON et les listes sauvegardées. */
enum class GameSystem { LEGION_V2, ARMADA_V15 }
enum class CardKind { LEGION_UNIT, LEGION_UPGRADE, ARMADA_SHIP, ARMADA_SQUADRON, ARMADA_UPGRADE, COMMANDER }
enum class LegionRank { COMMANDER, OPERATIVE, CORPS, SPECIAL_FORCES, SUPPORT, HEAVY }
enum class ArmadaSlot { COMMANDER, OFFENSIVE_RETROFIT, TURBOLASERS, WEAPONS_TEAM, SUPPORT_TEAM, DEFENSIVE_RETROFIT, ENGINEERING_TEAM, TITLE, OFFICER, FLEET_COMMAND, FLEET_SUPPORT, ION_CANNONS, ORDNANCE, EXPERIMENTAL_RETROFIT, SUPERWEAPON, FORCE, COMMAND, TRAINING, GEAR, ARMAMENT, PERSONNEL, COMMS, HEAVY_WEAPON, HARDPOINT, GRENADES, GENERATOR, DOCTRINE, COUNTERPART, PROGRAMMING, OTHER }

/** Représentation d'une carte de catalogue, directement désérialisable depuis le pipeline JSON. */
data class CardDefinition(
    val id: String,
    val gameSystem: GameSystem,
    val kind: CardKind,
    val name: String,
    val points: Int,
    val factionId: String,
    val legionRank: LegionRank? = null,
    val upgradeSlots: List<ArmadaSlot> = emptyList(),
    val allowedUpgradeSlots: List<ArmadaSlot> = emptyList(),
    val commander: Boolean = false,
    val unique: Boolean = false,
    val imageUrl: String? = null,
    val imageAssetPath: String? = null,
    val rulesText: String? = null,
    val names: Map<String, String> = emptyMap(),
    val shipStats: String? = null
) {
    fun displayName(locale: String = java.util.Locale.getDefault().language): String {
        return names[locale] ?: name
    }
}

data class CatalogDocument(
    val schemaVersion: Int = 1,
    val cards: List<CardDefinition> = emptyList()
)

data class ListEntry(
    val instanceId: String,
    val card: CardDefinition,
    val parentInstanceId: String? = null,
    val quantity: Int = 1,
    val chosenSlot: ArmadaSlot? = null
)

data class BuilderList(
    val id: String,
    val name: String,
    val gameSystem: GameSystem,
    val factionId: String,
    val pointsLimit: Int,
    val entries: List<ListEntry>
)

@Entity(tableName = "catalog_cards", indices = [Index("gameSystem"), Index("factionId")])
data class CatalogCardEntity(
    @PrimaryKey val id: String,
    val gameSystem: String,
    val kind: String,
    val name: String,
    val points: Int,
    val factionId: String,
    val legionRank: String?,
    /** Valeurs d'enum séparées par virgules; sérialisation simple, stable et inspectable. */
    val upgradeSlots: String,
    val allowedUpgradeSlots: String,
    val commander: Boolean,
    val unique: Boolean,
    val imageUrl: String?,
    val imageAssetPath: String?,
    val rulesText: String?,
    val names: String? = null,
    val shipStats: String? = null
) {
    fun toDefinition() = CardDefinition(
        id, GameSystem.valueOf(gameSystem), CardKind.valueOf(kind), name, points, factionId,
        legionRank?.let(LegionRank::valueOf), parseSlots(upgradeSlots), parseSlots(allowedUpgradeSlots),
        commander, unique, imageUrl, imageAssetPath, rulesText, parseJsonNames(names), shipStats
    )

    companion object {
        fun from(card: CardDefinition) = CatalogCardEntity(
            card.id, card.gameSystem.name, card.kind.name, card.name, card.points, card.factionId,
            card.legionRank?.name, card.upgradeSlots.joinToString(",") { it.name },
            card.allowedUpgradeSlots.joinToString(",") { it.name }, card.commander, card.unique,
            card.imageUrl, card.imageAssetPath, card.rulesText, toJsonNames(card.names), card.shipStats
        )
        private fun parseSlots(value: String) = value.split(',').filter(String::isNotBlank).map(ArmadaSlot::valueOf)
        private fun parseJsonNames(value: String?): Map<String, String> {
            if (value == null || value.isEmpty()) return emptyMap()
            return try { com.google.gson.Gson().fromJson(value, Map::class.java) as? Map<String, String> ?: emptyMap() } catch (_: Exception) { emptyMap() }
        }
        // Gson n'applique PAS les valeurs par défaut Kotlin : un champ absent du JSON
        // reste null (défaut JVM), pas emptyMap(). D'où le null-check obligatoire.
        private fun toJsonNames(names: Map<String, String>?): String? {
            if (names == null || names.isEmpty()) return null
            return try { com.google.gson.Gson().toJson(names) } catch (_: Exception) { null }
        }
    }
}

@Entity(tableName = "builder_lists")
data class BuilderListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val gameSystem: String,
    val factionId: String,
    val pointsLimit: Int,
    val updatedAt: Long
)

@Entity(tableName = "builder_entries", primaryKeys = ["instanceId"], indices = [Index("listId"), Index("cardId")])
data class BuilderEntryEntity(
    val instanceId: String,
    val listId: String,
    val cardId: String,
    val parentInstanceId: String?,
    val quantity: Int,
    val chosenSlot: String? = null
)
