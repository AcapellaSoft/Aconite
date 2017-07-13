package io.aconite.client

import io.aconite.Request
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KFunction
import kotlin.reflect.KType

internal class ModuleProxy {
    companion object Factory {
        private val map = ConcurrentHashMap<KType, ModuleProxy>()

        fun create(type: KType) = map.computeIfAbsent(type) {
            ModuleProxy()
        }
    }

    fun invoke(fn: KFunction<*>, url: String, request: Request, args: Array<Any?>): Any? {
        TODO("not implemented")
    }
}