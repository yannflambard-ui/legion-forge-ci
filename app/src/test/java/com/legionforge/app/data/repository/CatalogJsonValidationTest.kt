package com.legionforge.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.legionforge.app.data.model.CatalogDocument
import com.legionforge.app.data.model.CardDefinition
import com.legionforge.app.data.model.ArmadaSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogJsonValidationTest {

    private val gson = Gson()

    private fun loadCatalog(): CatalogDocument {
        // Try classpath first (for IDE or gradle test), then filesystem
        val json = try {
            javaClass.classLoader?.getResourceAsStream("catalog.json")
                ?.bufferedReader()?.readText()
        } catch (_: Exception) { null } ?: run {
            javaClass.classLoader?.getResourceAsStream("assets/catalog.json")
                ?.bufferedReader()?.readText()
        } ?: run {
            // Fallback: read from project root
            java.io.File("../app/src/main/assets/catalog.json").let {
                if (it.exists()) it.readText()
                else java.io.File("app/src/main/assets/catalog.json").readText()
            }
        }
        return gson.fromJson(json, CatalogDocument::class.java)
    }

    @Test
    fun catalogHasMinimumCards() {
        val doc = loadCatalog()
        println("Total cards: ${doc.cards.size}")
        assertTrue("Should have at least 500 cards, got ${doc.cards.size}", doc.cards.size >= 500)
    }

    @Test
    fun allCardsHaveValidGameSystem() {
        val doc = loadCatalog()
        val invalid = doc.cards.filter { it.gameSystem.name !in listOf("LEGION_V2", "ARMADA_V15") }
        assertEquals("All cards should have valid gameSystem", 0, invalid.size)
    }

    @Test
    fun allCardsHaveValidKind() {
        val doc = loadCatalog()
        val validKinds = setOf("LEGION_UNIT", "LEGION_UPGRADE", "ARMADA_SHIP", "ARMADA_SQUADRON", "ARMADA_UPGRADE", "COMMANDER")
        val invalid = doc.cards.filter { it.kind.name !in validKinds }
        assertEquals("All cards should have valid kind", 0, invalid.size)
    }

    @Test
    fun allPointsAreNonNegative() {
        val doc = loadCatalog()
        val negative = doc.cards.filter { it.points < 0 }
        assertEquals("All cards should have non-negative points", 0, negative.size)
    }

    @Test
    fun allSlotValuesAreValidEnum() {
        val doc = loadCatalog()
        val armadaSlotNames = ArmadaSlot.entries.map { it.name }.toSet()
        val unknownSlots = mutableListOf<String>()
        doc.cards.forEach { card ->
            card.upgradeSlots.forEach { slot ->
                if (slot.name !in armadaSlotNames) {
                    unknownSlots.add("${card.id}:upgradeSlots=${slot.name}")
                }
            }
            card.allowedUpgradeSlots.forEach { slot ->
                if (slot.name !in armadaSlotNames) {
                    unknownSlots.add("${card.id}:allowedUpgradeSlots=${slot.name}")
                }
            }
        }
        if (unknownSlots.isNotEmpty()) {
            println("UNKNOWN SLOTS: ${unknownSlots.take(20)}")
        }
        assertEquals("No unknown slot values", 0, unknownSlots.size)
    }

    @Test
    fun legionUnitsHaveRank() {
        val doc = loadCatalog()
        val legionUnits = doc.cards.filter { it.kind.name == "LEGION_UNIT" }
        val noRank = legionUnits.filter { it.legionRank == null }
        println("Legion units: ${legionUnits.size}, without rank: ${noRank.size}")
        if (noRank.isNotEmpty()) {
            println("Units without rank: ${noRank.take(10).map { it.name }}")
        }
        assertEquals("All legion units should have a rank", 0, noRank.size)
    }

    @Test
    fun armadaShipsHaveSlots() {
        val doc = loadCatalog()
        val ships = doc.cards.filter { it.kind.name == "ARMADA_SHIP" }
        val noSlots = ships.filter { it.allowedUpgradeSlots.isEmpty() }
        println("Armada ships: ${ships.size}, without slots: ${noSlots.size}")
        if (noSlots.isNotEmpty()) {
            println("Ships without slots: ${noSlots.take(5).map { it.name }}")
        }
        assertTrue("Most ships should have upgrade slots (${noSlots.size} without out of ${ships.size})", noSlots.size < ships.size / 2 + 5)
    }

    @Test
    fun cardNamesAreUniquePerKindAndFaction() {
        val doc = loadCatalog()
        // Check for remaining duplicates: same name + same points + same faction + same kind
        val seen = mutableSetOf<String>()
        val dupes = mutableListOf<String>()
        doc.cards.forEach { card ->
            val key = "${card.name.lowercase()}|${card.points}|${card.factionId}|${card.kind.name}"
            if (!seen.add(key)) {
                dupes.add("${card.name} (${card.points}pts, ${card.factionId}, ${card.kind.name})")
            }
        }
        if (dupes.isNotEmpty()) {
            println("REMAINING DUPLICATES: ${dupes.take(20)}")
        }
        assertEquals("No remaining strict duplicates", 0, dupes.size)
    }
}