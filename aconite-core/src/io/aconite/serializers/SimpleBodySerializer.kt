package io.aconite.serializers

import io.aconite.BodyBuffer
import io.aconite.BodySerializer
import io.aconite.Buffer
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

class SimpleBodySerializer : BodySerializer {
    object EmptySerializer : BodySerializer {
        override fun serialize(obj: Any?) = BodyBuffer(Buffer.wrap(""), "text/plain")
        override fun deserialize(body: BodyBuffer): Any? = null
    }

    object Factory: BodySerializer.Factory {
        override fun create(annotations: KAnnotatedElement, type: KType): BodySerializer? {
            if (type.classifier == Unit::class || type.classifier == Void::class) return EmptySerializer
            if (type.classifier != String::class) return null
            return SimpleBodySerializer()
        }
    }

    override fun serialize(obj: Any?) = BodyBuffer(
            content = Buffer.wrap(if (obj != null) obj as String else ""),
            contentType = "text/plain"
    )

    override fun deserialize(body: BodyBuffer) = body.content.string
}