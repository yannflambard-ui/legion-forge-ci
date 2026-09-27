package com.legionforge.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.legionforge.app.data.model.WikiSectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WikiDao {
    @Query("SELECT * FROM wiki_sections WHERE gameSystem = :system ORDER BY title")
    fun observeSections(system: String): Flow<List<WikiSectionEntity>>

    @Query("SELECT * FROM wiki_sections ORDER BY gameSystem, title")
    fun observeAllSections(): Flow<List<WikiSectionEntity>>

    /** Recherche plein texte sur titre + keywords + contenu. */
    @Query("""SELECT * FROM wiki_sections
              WHERE title LIKE '%' || :q || '%'
                 OR keywords LIKE '%' || :q || '%'
                 OR content LIKE '%' || :q || '%'
              ORDER BY gameSystem, title
              LIMIT 100""")
    fun searchSections(q: String): Flow<List<WikiSectionEntity>>

    @Query("SELECT * FROM wiki_sections WHERE id = :id LIMIT 1")
    suspend fun getSection(id: String): WikiSectionEntity?

    @Query("SELECT COUNT(*) FROM wiki_sections")
    suspend fun sectionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSections(sections: List<WikiSectionEntity>)
}
