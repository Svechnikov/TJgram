package io.svechnikov.tjgram.base.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Long,
    val name: String,
    val avatarUrl: String,
    val deviceToken: String,
    val userHash: String
) {

    companion object {
        const val DEVICE_TOKEN_UNDEFINED = ""
    }
}

