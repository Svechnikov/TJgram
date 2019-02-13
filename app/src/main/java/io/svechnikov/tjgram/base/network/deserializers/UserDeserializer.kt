package io.svechnikov.tjgram.base.network.deserializers

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import io.svechnikov.tjgram.base.db.entities.User
import java.lang.reflect.Type

class UserDeserializer : JsonDeserializer<User> {

    override fun deserialize(json: JsonElement?,
                             typeOfT: Type?,
                             context: JsonDeserializationContext?): User {
        val data = json!!.asJsonObject["result"].asJsonObject

        val id = data["id"].asLong
        val name = data["name"].asString
        val avatarUrl = data["avatar_url"].asString
        val userHash = data["user_hash"].asString

        return User(
            id,
            name,
            avatarUrl,
            User.DEVICE_TOKEN_UNDEFINED,
            userHash
        )
    }
}