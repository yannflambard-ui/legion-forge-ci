package com.legionforge.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Une section du wiki des règles officielles (Armada ou Legion).
 *  Le contenu reprend le texte officiel des PDF Atomic Mass Games.
 *  keywords = termes de recherche associés (séparés par des virgules en base). */
@Entity(tableName = "wiki_sections", indices = [Index("gameSystem")])
data class WikiSectionEntity(
    @PrimaryKey val id: String,
    val gameSystem: String,          // GameSystem.name : LEGION_V2 | ARMADA_V15
    val title: String,
    val keywords: String,            // liste séparée par des virgules
    val content: String
) {
    fun keywordList(): List<String> = keywords.split(',').map { it.trim() }.filter { it.isNotEmpty() }
}

/** Document JSON du wiki (assets/wiki.json) : liste de sections par jeu. */
data class WikiDocument(
    val schemaVersion: Int = 1,
    val sections: List<WikiSection> = emptyList()
)

data class WikiSection(
    val id: String,
    val gameSystem: String,
    val title: String,
    val keywords: List<String> = emptyList(),
    val content: String
)
