package io.aconite.serializers

import io.aconite.BadRequestException
import io.aconite.StringSerializer
import java.lang.reflect.InvocationTargetException
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KType

enum class SameSiteType {
    STRICT,
    LAX
}

data class Cookie(
        val data: Map<String, String>,
        val expires: Date? = null,
        val maxAge: Long? = null,
        val domain: String? = null,
        val path: String? = null,
        val sameSite: SameSiteType? = null,
        val secure: Boolean = false,
        val httpOnly: Boolean = false
)

private val expireDateFormat = SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz", Locale.ROOT)

private val EXPIRES_KEY = "Expires"
private val MAX_AGE_KEY = "MaxAge"
private val DOMAIN_KEY = "Domain"
private val PATH_KEY = "Path"
private val SAME_SITE_KEY = "SameSite"
private val SECURE_FLAG = "Secure"
private val HTTP_ONLY_FLAG = "HttpOnly"

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
        if (obj !is Cookie) return null
        return buildString {
            obj.data.forEach { k, v -> appendToCookie(k, v) }
            appendToCookie(EXPIRES_KEY, obj.expires?.let { expireDateFormat.format(it) })
            appendToCookie(MAX_AGE_KEY, obj.maxAge?.toString())
            appendToCookie(DOMAIN_KEY, obj.domain)
            appendToCookie(PATH_KEY, obj.path)
            appendToCookie(SAME_SITE_KEY, obj.sameSite?.toString())
            if (obj.secure) append("$SECURE_FLAG; ")
            if (obj.httpOnly) append("$HTTP_ONLY_FLAG; ")
        }
    }

    override fun deserialize(s: String): Cookie {
        val parts = s.split(";")
                .map { it.trim().split("=") }

        val kvs = parts
                .filter { it.size == 2 }
                .map { (k, v) -> Pair(k, v) }
                .toMap()

        val flags = parts
                .filter { it.size == 1 }
                .map { it.first() }
                .toSet()

        return Cookie(
                data = kvs,
                expires = kvs[EXPIRES_KEY]?.let { expireDateFormat.parse(it) },
                maxAge = kvs[MAX_AGE_KEY]?.toLong(),
                domain = kvs[EXPIRES_KEY],
                path = kvs[PATH_KEY],
                sameSite = kvs[SAME_SITE_KEY]?.let { SameSiteType.valueOf(it) },
                secure = flags.contains(SECURE_FLAG),
                httpOnly = flags.contains(HTTP_ONLY_FLAG)
        )
    }

    private fun StringBuilder.appendToCookie(key: String, value: String?) {
        if (value != null) {
            append(key)
            append('=')
            append(value)
            append("; ")
        }
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