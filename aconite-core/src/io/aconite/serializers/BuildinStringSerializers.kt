package io.aconite.serializers

import io.aconite.BadRequestException
import io.aconite.StringSerializer
import java.lang.reflect.InvocationTargetException
import java.time.Instant
import java.util.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KType

data class Cookie(
        val data: Map<String, String>
)

private inline fun <reified T: Any> factoryFor(serializer: StringSerializer): StringSerializer.Factory {
    val cls = T::class

    return object : StringSerializer.Factory {
        override fun create(annotations: KAnnotatedElement, type: KType)
                = if (cls == type.classifier) serializer else null
    }
}

private inline fun <reified T: Any> factoryFor(noinline deserializer: (String) -> T): StringSerializer.Factory {
    return factoryFor<T>(object : StringSerializer {
        override fun serialize(obj: Any?) = obj?.toString()

        override fun deserialize(s: String) = try {
            deserializer(s)
        } catch (ex: IllegalArgumentException) {
            throw BadRequestException(cause = ex)
        }
    })
}

val DefaultStringSerializer = factoryFor { it }
val ByteStringSerializer = factoryFor(String::toByte)
val ShortStringSerializer = factoryFor(String::toShort)
val IntegerStringSerializer = factoryFor(String::toInt)
val LongStringSerializer = factoryFor(String::toLong)
val FloatStringSerializer = factoryFor(String::toFloat)
val DoubleStringSerializer = factoryFor(String::toDouble)
val BooleanStringSerializer = factoryFor(String::toBoolean)
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
            val ms = s.toLong()
            val instant = Instant.ofEpochMilli(ms)
            return Date.from(instant)
        } catch (ex: IllegalArgumentException) {
            throw BadRequestException(cause = ex)
        }
    }
})

val CookieStringSerializer = factoryFor<Cookie>(object : StringSerializer {
    override fun serialize(obj: Any?): String? {
        TODO("not implemented")
    }

    override fun deserialize(s: String): Cookie {
        // todo: parse other fields
        val data = s.split(";")
                .map { it.trim().split("=") }
                .filter { it.size == 2 }
                .map { Pair(it[0], it[1]) }
                .toMap()
        return Cookie(data)
    }
})

class EnumStringSerializer(clazz: Class<*>) : StringSerializer {
    private val valueOfFn = clazz.getMethod("valueOf", String::class.java)

    object Factory : StringSerializer.Factory {
        override fun create(annotations: KAnnotatedElement, type: KType): StringSerializer? {
            val clazz = (type.classifier as? KClass<*>)?.java
            if (clazz?.isEnum == true) {
                return EnumStringSerializer(clazz)
            }
            return null
        }
    }

    override fun deserialize(s: String): Any? = try {
        valueOfFn.invoke(null, s)
    } catch (ex: InvocationTargetException) {
        if (ex.cause is IllegalArgumentException)
            throw BadRequestException(cause = ex.cause)
        else
            throw ex
    }

    override fun serialize(obj: Any?): String? = obj.toString()
}

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
        DateStringSerializer,
        CookieStringSerializer,
        EnumStringSerializer.Factory
)