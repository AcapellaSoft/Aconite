package io.aconite.server.serializers

import io.aconite.server.StringSerializer
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

class SimpleStringSerializer: StringSerializer {
    object Factory: StringSerializer.Factory {
        override fun create(annotations: KAnnotatedElement, type: KType): StringSerializer? {
            if (type.classifier != String::class) return null
            return SimpleStringSerializer()
        }
    }

    override fun serialize(obj: Any?) = if (obj != null) obj as String else ""
    override fun deserialize(s: String) = s
}