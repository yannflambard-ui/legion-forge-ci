package com.legionforge.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.legionforge.app.data.model.ArmyList
import com.legionforge.app.data.model.ArmyUnit
import com.legionforge.app.data.model.Faction
import com.legionforge.app.data.model.Keyword
import com.legionforge.app.data.model.OwnedUnit
import com.legionforge.app.data.model.UnitEntity
import com.legionforge.app.data.model.UnitKeywordCrossRef
import com.legionforge.app.data.model.UpgradeSlot

@Database(
    entities = [
        Faction::class,
        UnitEntity::class,
        UpgradeSlot::class,
        Keyword::class,
        UnitKeywordCrossRef::class,
        ArmyList::class,
        ArmyUnit::class,
        OwnedUnit::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LegionForgeDatabase : RoomDatabase() {
    abstract fun gameDataDao(): GameDataDao
    abstract fun armyListDao(): ArmyListDao

    companion object {
        @Volatile
        private var INSTANCE: LegionForgeDatabase? = null

        fun getInstance(context: Context): LegionForgeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LegionForgeDatabase::class.java,
                    "legionforge.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
