package io.aconite.server.serializers

import io.aconite.BadRequestException
import io.aconite.StringSerializer
import java.time.Instant
import java.util.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

private inline fun <reified T: Any> factoryFor(serializer: StringSerializer): StringSerializer.Factory {
    val cls = T::class

    return object : StringSerializer.Factory {
        override fun create(annotations: KAnnotatedElement, type: KType)
                = if (cls == type.classifier) serializer else null
    }
}

private inline fun <reified T: Any> factoryFor(noinline deserializer: (String) -> T): StringSerializer.Factory {
    return factoryFor<T>(object: StringSerializer {
        override fun serialize(obj: Any?) = obj.toString()

        override fun deserialize(s: String) = try {
            deserializer(s)
        } catch (ex: IllegalArgumentException) {
            throw BadRequestException(cause = ex)
        }
    })
}

val DefaultStringSerializer = factoryFor { it }
val ByteStringSerializer = factoryFor(java.lang.Byte::parseByte)
val ShortStringSerializer = factoryFor(java.lang.Short::parseShort)
val IntegerStringSerializer = factoryFor(java.lang.Integer::parseInt)
val LongStringSerializer = factoryFor(java.lang.Long::parseLong)
val FloatStringSerializer = factoryFor(java.lang.Float::parseFloat)
val DoubleStringSerializer = factoryFor(java.lang.Double::parseDouble)
val BooleanStringSerializer = factoryFor(java.lang.Boolean::parseBoolean)
val CharStringSerializer = factoryFor(String::first)
val UuidStringSerializer = factoryFor(UUID::fromString)

val DateStringSerializer = factoryFor<Date>(object : StringSerializer {
    override fun serialize(obj: Any?): String? {
        val date = obj as Date
        val instant = date.toInstant()
        val ms = instant.toEpochMilli()
        return ms.toString()
    }

    override fun deserialize(s: String): Any? {
        try {
            val ms = java.lang.Long.parseLong(s)
            val instant = Instant.ofEpochMilli(ms)
            return Date.from(instant)
        } catch (ex: IllegalArgumentException) {
            throw BadRequestException(cause = ex)
        }
    }
})

val BuildInStringSerializers = oneOf(
        DefaultStringSerializer,
        ByteStringSerializer,
        ShortStringSerializer,
        IntegerStringSerializer,
        LongStringSerializer,
        FloatStringSerializer,
        DoubleStringSerializer,
        BooleanStringSerializer,
        CharStringSerializer,
        UuidStringSerializer,
        DateStringSerializer
)