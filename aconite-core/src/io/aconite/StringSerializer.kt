package io.aconite

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

interface StringSerializer {
    interface Factory {
        fun create(annotations: KAnnotatedElement, type: KType): StringSerializer?
    }

    fun serialize(obj: Any?): String?
    fun deserialize(s: String): Any?
}