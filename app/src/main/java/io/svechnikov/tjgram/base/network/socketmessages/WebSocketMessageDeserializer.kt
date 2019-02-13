package io.svechnikov.tjgram.base.network.socketmessages

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import io.svechnikov.tjgram.base.data.WebSocketMessage
import java.lang.reflect.Type

class WebSocketMessageDeserializer : JsonDeserializer<WebSocketMessage> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): WebSocketMessage {

        val data = json!!.asJsonObject

        return WebSocketMessage(
            type = data["type"].asString,
            contentId = data["content_id"].asLong,
            count = data["count"].asInt,
            id = data["id"].asLong,
            state = data["state"].asInt,
            userHash = data["user_hash"].asString
        )
    }
}