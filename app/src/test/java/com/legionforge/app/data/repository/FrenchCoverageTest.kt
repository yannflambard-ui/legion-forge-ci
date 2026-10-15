package com.legionforge.app.data.repository

import com.google.gson.Gson
import com.legionforge.app.data.model.CatalogDocument
import com.legionforge.app.data.model.CardDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Couverture des cartes FR (imageAssetPaths["fr"]).
 *
 * Vérifie :
 *  1. chaque image FR référencée dans le catalogue existe réellement sur disque ;
 *  2. le gap de couverture des unités Legion (LEGION_UNIT) sans image FR est borné
 *     (on ne régresse pas, et on tend vers 0).
 */
class FrenchCoverageTest {

    private val gson = Gson()

    private fun loadCatalog(): CatalogDocument {
        val json = try {
            javaClass.classLoader?.getResourceAsStream("catalog.json")
                ?.bufferedReader()?.readText()
        } catch (_: Exception) { null } ?: run {
            javaClass.classLoader?.getResourceAsStream("assets/catalog.json")
                ?.bufferedReader()?.readText()
        } ?: run {
            java.io.File("../app/src/main/assets/catalog.json").let {
                if (it.exists()) it.readText()
                else java.io.File("app/src/main/assets/catalog.json").readText()
            }
        }
        return gson.fromJson(json, CatalogDocument::class.java)
    }

    /** Racine des assets, résolue depuis le cwd du test (app/ ou racine du repo). */
    private fun assetsRoot(): File {
        val candidates = listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
            File("../app/src/main/assets"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Impossible de localiser app/src/main/assets")
    }

    @Test
    fun everyReferencedFrenchImageExistsOnDisk() {
        val doc = loadCatalog()
        val root = assetsRoot()
        val missing = mutableListOf<String>()
        var referenced = 0
        doc.cards.forEach { card ->
            val fr = card.imageAssetPaths?.get("fr")
            if (fr != null) {
                referenced++
                if (!File(root, fr).isFile) {
                    missing.add("${card.name} -> $fr")
                }
            }
        }
        if (missing.isNotEmpty()) {
            println("IMAGES FR MANQUANTES (${missing.size}):")
            missing.take(30).forEach { println("  $it") }
        }
        println("Images FR référencées: $referenced, manquantes: ${missing.size}")
        assertEquals("Toute image FR référencée doit exister sur disque", 0, missing.size)
    }

    @Test
    fun legionUnitsFrenchCoverageDoesNotRegress() {
        val doc = loadCatalog()
        val units = doc.cards.filter { it.kind.name == "LEGION_UNIT" }
        val withFr = units.filter { it.imageAssetPaths?.get("fr") != null }
        val withoutFr = units.filter { it.imageAssetPaths?.get("fr") == null }

        println("Unités Legion: ${units.size} | avec FR: ${withFr.size} | sans FR: ${withoutFr.size}")
        println("Couverture FR unités: ${(withFr.size * 100.0 / units.size).roundToInt()}%")

        // Barre de non-régression : on ne doit pas perdre de couverture par rapport à l'existant.
        // (113/172 au moment de l'écriture du test.)
        assertTrue(
            "Couverture FR unités en régression: ${withFr.size} (attendu >= 113)",
            withFr.size >= 113
        )

        // Le gap restant est listé pour piloter le chantier d'amélioration.
        if (withoutFr.isNotEmpty()) {
            println("Unités SANS image FR (${withoutFr.size}):")
            withoutFr.sortedBy { it.name }.forEach { println("  - ${it.name} [${it.factionId}]") }
        }
    }

    @Test
    fun everyFrenchImagePathIsUnderFrFolder() {
        val doc = loadCatalog()
        val bad = doc.cards
            .mapNotNull { it.imageAssetPaths?.get("fr")?.let { p -> it.name to p } }
            .filter { (_, p) -> !p.contains("/fr/") }
        assertEquals("Toute image FR doit être sous un dossier /fr/", 0, bad.size)
    }

    private fun Int.roundToInt(): Int = Math.round(this.toFloat())
}
