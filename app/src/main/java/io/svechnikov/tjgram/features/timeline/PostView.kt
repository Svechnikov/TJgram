package io.svechnikov.tjgram.features.timeline

data class PostView(val postId: Long,
                    val title: String,
                    val intro: CharSequence?,
                    val userName: String,
                    val userId: Long,
                    val userAvatarUrl: String,
                    val thumbnailUrl: String,
                    val mediaRatio: Float,
                    val videoUrl: String?,
                    val isLiked: Int,
                    val likes: CharSequence,
                    var videoPosition: Long = 0) {

    fun isVideo(): Boolean {
        return videoUrl != null
    }
}