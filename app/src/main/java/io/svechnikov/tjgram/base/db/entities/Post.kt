package io.svechnikov.tjgram.base.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts",
    indices = [
        Index(value = ["sorting", "postId"], unique = true)
    ]
)
data class Post(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val postId: Long,
    val title: String,
    val intro: String,
    val date: Int,
    val userName: String,
    val userId: Long,
    val userAvatarUrl: String,
    val thumbnailUrl: String?,
    val videoUrl: String?,
    val mediaRatio: Float,
    val isLiked: Int,
    val likes: Int,
    val sorting: Sorting,
    val isFinal: Boolean) {

    enum class Sorting {
        NOT_SPECIFIED,
        NEW,
        TOP_WEEK,
        TOP_MONTH,
        TOP_YEAR,
        TOP_ALL
    }
}