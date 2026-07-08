package com.legionforge.app.domain

import com.legionforge.app.data.model.UnitEntity
import com.legionforge.app.data.model.UnitRank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArmyListValidatorTest {

    private fun unit(id: String, rank: UnitRank, points: Int, min: Int = 0, max: Int = 6) =
        UnitEntity(
            id = id,
            factionId = "rebel",
            name = id,
            points = points,
            rank = rank,
            minInArmy = min,
            maxInArmy = max
        )

    @Test
    fun `valid list with commander and two corps passes`() {
        val entries = listOf(
            unit("cmd", UnitRank.COMMANDER, 70) to 1,
            unit("corps", UnitRank.CORPS, 44, min = 2) to 2
        )
        val result = ArmyListValidator.validate(entries, pointsLimit = 1000)
        assertTrue(result.isValid)
        assertEquals(70 + 44 * 2, result.totalPoints)
    }

    @Test
    fun `list without commander is invalid`() {
        val entries = listOf(unit("corps", UnitRank.CORPS, 44, min = 2) to 2)
        val result = ArmyListValidator.validate(entries, pointsLimit = 1000)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Commandant") })
    }

    @Test
    fun `list exceeding points limit is invalid`() {
        val entries = listOf(
            unit("cmd", UnitRank.COMMANDER, 900) to 1,
            unit("corps", UnitRank.CORPS, 100, min = 2) to 2
        )
        val result = ArmyListValidator.validate(entries, pointsLimit = 1000)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("dépasse") })
    }

    @Test
    fun `too many commanders is invalid`() {
        val entries = listOf(
            unit("cmd1", UnitRank.COMMANDER, 70, max = 2) to 1,
            unit("cmd2", UnitRank.COMMANDER, 70, max = 2) to 2,
            unit("corps", UnitRank.CORPS, 44, min = 2) to 2
        )
        val result = ArmyListValidator.validate(entries, pointsLimit = 1000)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Maximum 2 Commandants") })
    }
}
