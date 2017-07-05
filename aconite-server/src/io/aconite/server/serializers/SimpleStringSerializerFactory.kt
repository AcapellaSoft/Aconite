package io.aconite.server.serializers

import io.aconite.StringSerializer
import java.util.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KType

class SimpleStringSerializerFactory : StringSerializer.Factory {
    private val serializers = mutableMapOf<KClass<*>, StringSerializer>()

    init {
        setSerializer { it }
        setSerializer { java.lang.Byte.parseByte(it) }
        setSerializer { java.lang.Short.parseShort(it) }
        setSerializer { java.lang.Integer.parseInt(it) }
        setSerializer { java.lang.Long.parseLong(it) }
        setSerializer { java.lang.Float.parseFloat(it) }
        setSerializer { java.lang.Double.parseDouble(it) }
        setSerializer { java.lang.Boolean.parseBoolean(it) }
        setSerializer { it.first() }
        setSerializer { UUID.fromString(it) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> setSerializer(cls: KClass<T>, serializer: (T) -> String, deserializer: (String) -> T) {
        serializers[cls] = object : StringSerializer {
            override fun deserialize(s: String) = deserializer(s)
            override fun serialize(obj: Any?) = obj?.let { serializer(it as T) }
        }
    }

    inline fun <reified T: Any> setSerializer(noinline deserialize: (String) -> T) {
        setSerializer(T::class, { it.toString() }, deserialize)
    }

    inline fun <reified T: Any> setSerializer(noinline serialize: (T) -> String, noinline deserialize: (String) -> T) {
        setSerializer(T::class, serialize, deserialize)
    }

    override fun create(annotations: KAnnotatedElement, type: KType): StringSerializer? {
        return serializers[type.classifier]
    }
}