package io.aconite.server.serializers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import io.aconite.BadRequestException
import io.aconite.UnsupportedMediaTypeException
import io.aconite.server.BodyBuffer
import io.aconite.server.BodySerializer
import io.aconite.server.Buffer
import io.aconite.utils.toJavaType
import java.lang.reflect.Type
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

class GsonBodySerializer(val gson: Gson, val type: Type): BodySerializer {

    class Factory(val gson: Gson = Gson()): BodySerializer.Factory {
        constructor(builder: GsonBuilder): this(builder.create())
        override fun create(annotations: KAnnotatedElement, type: KType) = GsonBodySerializer(gson, type.toJavaType())
    }

    override fun serialize(obj: Any?) = BodyBuffer(
            content = Buffer.wrap(gson.toJson(obj)),
            contentType = "application/json"
    )

    override fun deserialize(body: BodyBuffer): Any? {
        if (body.contentType.toLowerCase() != "application/json")
            throw UnsupportedMediaTypeException("Only 'application/json' media type supported")
        try {
            return gson.fromJson(body.content.string, type)
        } catch (ex: JsonParseException) {
            throw BadRequestException("Bad JSON format. ${ex.message}")
        }
    }
}