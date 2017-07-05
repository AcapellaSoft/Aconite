package io.aconite

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

interface BodySerializer {
    interface Factory {
        fun create(annotations: KAnnotatedElement, type: KType): BodySerializer?
    }

    fun serialize(obj: Any?): BodyBuffer
    fun deserialize(body: BodyBuffer): Any?
}