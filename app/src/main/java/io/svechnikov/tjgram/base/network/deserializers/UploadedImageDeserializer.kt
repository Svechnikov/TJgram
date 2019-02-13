package io.svechnikov.tjgram.base.network.deserializers

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import io.svechnikov.tjgram.base.data.UploadedImage
import java.lang.reflect.Type

class UploadedImageDeserializer : JsonDeserializer<UploadedImage> {
    override fun deserialize(json: JsonElement?,
                             typeOfT: Type?,
                             context: JsonDeserializationContext?): UploadedImage {
        val resultData = json!!.asJsonObject["result"].asJsonArray[0].asJsonObject
        val resultType = resultData["type"].asString

        val data = resultData["data"].asJsonObject
        val uuid = data["uuid"].asString
        val width = data["width"].asInt
        val height = data["height"].asInt
        val size = data["size"].asInt
        val type = data["type"].asString
        val color = data["color"].asString

        val uploadedImageData = UploadedImage.Data(
            uuid = uuid,
            width = width,
            height = height,
            size = size,
            type = type,
            color = color
        )

        return UploadedImage(
            type = resultType,
            data = uploadedImageData
        )
    }
}