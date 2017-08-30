package io.aconite.serializers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import io.aconite.*
import io.aconite.utils.toJavaType
import java.lang.reflect.Type
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

class GsonBodySerializer(private val gson: Gson, val type: Type): BodySerializer {

    class Factory(private val gson: Gson = Gson()): BodySerializer.Factory {
        constructor(builder: GsonBuilder): this(builder.create())
        override fun create(annotations: KAnnotatedElement, type: KType) = GsonBodySerializer(gson, type.toJavaType())
    }

    override fun serialize(obj: Any?) = Buffer.wrap(gson.toJson(obj, type))

    override fun deserialize(body: Buffer): Any? {
        if (body.bytes.isEmpty()) return null

        try {
            return gson.fromJson(body.string, type)
        } catch (ex: JsonParseException) {
            throw BadRequestException("Bad JSON format. ${ex.message}")
        }
    }
}