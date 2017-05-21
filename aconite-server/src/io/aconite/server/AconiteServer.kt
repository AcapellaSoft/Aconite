package io.aconite.server

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KCallable
import kotlin.reflect.KType

interface BodySerializer {
    interface Factory {
        fun create(annotations: KAnnotatedElement, type: KType): BodySerializer?
    }

    fun serialize(obj: Any?): BodyBuffer?
    fun deserialize(body: BodyBuffer?): Any?
}

interface StringSerializer {
    interface Factory {
        fun create(annotations: KAnnotatedElement, type: KType): StringSerializer?
    }

    fun serialize(obj: Any?): String
    fun deserialize(s: String): Any?
}

interface CallAdapter {
    fun adapt(fn: KCallable<*>): KCallable<*>?
}

interface MethodFilter {
    fun predicate(fn: KCallable<*>): Boolean
}

class AconiteServer(
        val bodySerializer: BodySerializer,
        val stringSerializer: StringSerializer,
        val callAdapter: CallAdapter,
        val methodFilter: MethodFilter
) {

}