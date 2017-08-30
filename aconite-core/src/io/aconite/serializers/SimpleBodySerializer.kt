package io.aconite.serializers

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

    override fun serialize(obj: Any?) = Buffer.wrap(if (obj != null) obj as String else "")

    override fun deserialize(body: Buffer) = body.string
}