package io.aconite.server

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KFunction
import kotlin.reflect.KType

interface BodySerializer {
    interface Factory {
        fun create(annotations: KAnnotatedElement, type: KType): BodySerializer?
    }

    fun serialize(obj: Any?): BodyBuffer
    fun deserialize(body: BodyBuffer): Any?
}

interface StringSerializer {
    interface Factory {
        fun create(annotations: KAnnotatedElement, type: KType): StringSerializer?
    }

    fun serialize(obj: Any?): String
    fun deserialize(s: String): Any?
}

interface CallAdapter {
    fun adapt(fn: KFunction<*>): KFunction<*>?
}

interface MethodFilter {
    fun predicate(fn: KFunction<*>): Boolean
}

class AconiteServerException(message: String): Exception(message)

class AconiteServer(
        val bodySerializer: BodySerializer.Factory,
        val stringSerializer: StringSerializer.Factory,
        val callAdapter: CallAdapter,
        val methodFilter: MethodFilter
) {
}