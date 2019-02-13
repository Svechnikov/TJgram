package io.svechnikov.tjgram.base.network.deserializers

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import io.svechnikov.tjgram.base.data.LikePostResult
import java.lang.reflect.Type

class LikePostResultDeserializer : JsonDeserializer<LikePostResult> {
    override fun deserialize(json: JsonElement?,
                             typeOfT: Type?,
                             context: JsonDeserializationContext?): LikePostResult {
        val data = json!!.asJsonObject["result"].asJsonObject

        return LikePostResult(data["summ"].asInt, data["is_liked"].asInt)
    }
}