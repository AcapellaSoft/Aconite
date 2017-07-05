package io.aconite.server.serializers

import io.aconite.BodyBuffer
import io.aconite.BodySerializer
import io.aconite.Buffer
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

class SimpleBodySerializer: BodySerializer {
    object Factory: BodySerializer.Factory {
        override fun create(annotations: KAnnotatedElement, type: KType): BodySerializer? {
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