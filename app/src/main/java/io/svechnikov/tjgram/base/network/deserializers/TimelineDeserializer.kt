package io.svechnikov.tjgram.base.network.deserializers

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import io.svechnikov.tjgram.base.db.entities.Post
import timber.log.Timber
import java.lang.reflect.Type

class TimelineDeserializer : JsonDeserializer<List<Post>> {
    override fun deserialize(json: JsonElement?,
                             typeOfT: Type?,
                             context: JsonDeserializationContext?): List<Post> {
        val posts = arrayListOf<Post>()

        val items = json!!.asJsonObject.get("result").asJsonArray

        for (item in items) {
            val data = item.asJsonObject

            val postId = data["id"].asLong
            val title = data["title"].asString
            val intro = data["intro"].asString
            val date = data["date"].asInt

            val author = data["author"].asJsonObject
            val userId = author["id"].asLong
            val userName = author["name"].asString
            val userAvatarUrl = author["avatar_url"].asString

            var thumbnailUrl: String? = null
            var mediaRatio = 0.0f
            var videoUrl: String? = null

            val cover = data["cover"]
            if (!cover.isJsonNull) {
                thumbnailUrl = cover.asJsonObject["thumbnailUrl"].asString

                val size = cover.asJsonObject["size"].asJsonObject
                val width = size["width"].asFloat
                val height = size["height"].asFloat
                mediaRatio = width / height

                val additionalData = cover.asJsonObject["additionalData"].asJsonObject
                if (additionalData["type"].asString == "gif") {
                    videoUrl = cover.asJsonObject["url"].asString
                }
            }

            val likes = data["likes"].asJsonObject

            val isLiked = likes["is_liked"].asInt
            val likesSumm = likes["summ"].asInt

            posts.add(
                Post(
                    postId = postId,
                    title = title,
                    intro = intro,
                    date = date,
                    userId = userId,
                    userName = userName,
                    userAvatarUrl = userAvatarUrl,
                    thumbnailUrl = thumbnailUrl,
                    videoUrl = videoUrl,
                    mediaRatio = mediaRatio,
                    isLiked = isLiked,
                    likes = likesSumm,
                    sorting = Post.Sorting.NOT_SPECIFIED,
                    isFinal = false,
                    id = 0
                )
            )
        }

        return posts
    }
}