package io.aconite.server

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.createType

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
    private val modules = mutableListOf<RootHandler>()

    fun <T: Any> register(obj: T, iface: KClass<T>) {
        modules.add(RootHandler(this, obj, iface.createType()))
    }

    suspend fun accept(url: String, request: Request): Response? {
        for (router in modules) {
            val (response, error) = router.accept(url, request)
            if (error != null) return error.toResponse()
            if (response != null) return response
        }
        return null
    }
}