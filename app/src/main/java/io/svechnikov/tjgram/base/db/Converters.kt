package io.svechnikov.tjgram.base.db

import androidx.room.TypeConverter
import io.svechnikov.tjgram.base.db.entities.Post

class Converters {
    @TypeConverter
    fun toPostSorting(value: Int): Post.Sorting {
        return Post.Sorting.values()[value]
    }

    @TypeConverter
    fun fromPostSorting(sorting: Post.Sorting): Int  {
        return Post.Sorting.values().indexOf(sorting)
    }
}