package com.legionforge.app.domain

import com.legionforge.app.data.model.UnitEntity
import com.legionforge.app.data.model.UnitRank

/** Résultat de validation d'une liste d'armée. */
data class ValidationResult(
    val isValid: Boolean,
    val totalPoints: Int,
    val pointsLimit: Int,
    val errors: List<String>
)

/**
 * Règles de validation SW Legion v2 pour une liste d'armée (MVP) :
 * - Points totaux <= limite (1000 par défaut)
 * - Au moins 2 unités Corps
 * - Max 2 Commandants
 * - Respect des min/max par unité
 */
object ArmyListValidator {

    fun validate(
        entries: List<Pair<UnitEntity, Int>>, // unité + quantité présente dans la liste
        pointsLimit: Int = 1000
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val totalPoints = entries.sumOf { (unit, qty) -> unit.points * qty }

        if (totalPoints > pointsLimit) {
            errors.add("Total de points ($totalPoints) dépasse la limite ($pointsLimit).")
        }

        val corpsCount = entries.filter { it.first.rank == UnitRank.CORPS }.sumOf { it.second }
        if (corpsCount < 2) {
            errors.add("Il faut au moins 2 unités de rang Corps (actuellement $corpsCount).")
        }

        val commanderCount = entries.filter { it.first.rank == UnitRank.COMMANDER }.sumOf { it.second }
        if (commanderCount > 2) {
            errors.add("Maximum 2 Commandants autorisés (actuellement $commanderCount).")
        }
        if (commanderCount < 1) {
            errors.add("Il faut au moins 1 Commandant dans la liste.")
        }

        entries.forEach { (unit, qty) ->
            if (qty < unit.minInArmy) {
                errors.add("${unit.name} : minimum ${unit.minInArmy} requis (actuellement $qty).")
            }
            if (qty > unit.maxInArmy) {
                errors.add("${unit.name} : maximum ${unit.maxInArmy} autorisé (actuellement $qty).")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            totalPoints = totalPoints,
            pointsLimit = pointsLimit,
            errors = errors
        )
    }
}
