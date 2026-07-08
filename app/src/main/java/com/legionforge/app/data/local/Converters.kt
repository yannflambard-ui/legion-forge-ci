package com.legionforge.app.data.local

import androidx.room.TypeConverter
import com.legionforge.app.data.model.UnitRank

class Converters {
    @TypeConverter
    fun fromRank(rank: UnitRank): String = rank.name

    @TypeConverter
    fun toRank(value: String): UnitRank = UnitRank.valueOf(value)
}
