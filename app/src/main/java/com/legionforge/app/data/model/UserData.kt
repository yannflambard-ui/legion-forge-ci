package com.legionforge.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** Une liste d'armée créée par l'utilisateur. */
@Entity(tableName = "army_lists")
data class ArmyList(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val factionId: String,
    val battleForceId: String? = null,
    val pointsLimit: Int = 1000,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** Une unité ajoutée dans une liste (instance concrète, avec ses upgrades). */
@Entity(tableName = "army_units")
data class ArmyUnit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val armyListId: String,
    val unitId: String,
    /** Upgrades sélectionnées, sérialisées en CSV d'ids pour le MVP. */
    val upgradeIds: String = ""
)

/** Unité possédée par le joueur (collection tracker, phase 2). */
@Entity(tableName = "owned_units", primaryKeys = ["unitId"])
data class OwnedUnit(
    val unitId: String,
    val quantity: Int = 1
)
