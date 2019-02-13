package io.svechnikov.tjgram.base.network.deserializers

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import io.svechnikov.tjgram.base.exceptions.ServiceException
import java.lang.reflect.Type

class ServiceErrorDeserializer : JsonDeserializer<ServiceException> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ServiceException {
        val data = json!!.asJsonObject

        return ServiceException(
            data["error"].asJsonObject["code"].asInt,
            data["message"].asString
        )
    }
}