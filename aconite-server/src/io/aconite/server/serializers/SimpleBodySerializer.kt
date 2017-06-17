package io.aconite.server.serializers

import io.aconite.server.BodyBuffer
import io.aconite.server.BodySerializer
import java.nio.ByteBuffer
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
            content = ByteBuffer.wrap((if (obj != null) obj as String else "").toByteArray()),
            contentType = "text/plain"
    )

    override fun deserialize(body: BodyBuffer) = String((body.content as ByteBuffer).array())
}