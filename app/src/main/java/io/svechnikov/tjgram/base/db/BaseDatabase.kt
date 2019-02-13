package io.svechnikov.tjgram.base.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.svechnikov.tjgram.base.db.dao.PostDao
import io.svechnikov.tjgram.base.db.dao.UserDao
import io.svechnikov.tjgram.base.db.entities.Post
import io.svechnikov.tjgram.base.db.entities.User

@Database(
    entities = [Post::class, User::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BaseDatabase : RoomDatabase() {
    abstract fun posts(): PostDao
    abstract fun user(): UserDao
}