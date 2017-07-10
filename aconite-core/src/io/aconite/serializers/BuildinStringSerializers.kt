package io.aconite.serializers

import io.aconite.BadRequestException
import io.aconite.StringSerializer
import java.lang.Boolean
import java.lang.Byte
import java.lang.Double
import java.lang.Float
import java.lang.Long
import java.lang.Short
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
    return factoryFor<T>(object : StringSerializer {
        override fun serialize(obj: Any?) = obj.toString()

        override fun deserialize(s: String) = try {
            deserializer(s)
        } catch (ex: IllegalArgumentException) {
            throw BadRequestException(cause = ex)
        }
    })
}

val DefaultStringSerializer = factoryFor { it }
val ByteStringSerializer = factoryFor(Byte::parseByte)
val ShortStringSerializer = factoryFor(Short::parseShort)
val IntegerStringSerializer = factoryFor(Integer::parseInt)
val LongStringSerializer = factoryFor(Long::parseLong)
val FloatStringSerializer = factoryFor(Float::parseFloat)
val DoubleStringSerializer = factoryFor(Double::parseDouble)
val BooleanStringSerializer = factoryFor(Boolean::parseBoolean)
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
            val ms = Long.parseLong(s)
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