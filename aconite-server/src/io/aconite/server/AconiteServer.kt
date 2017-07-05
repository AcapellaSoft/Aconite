package io.aconite.server

import io.aconite.BodySerializer
import io.aconite.Request
import io.aconite.Response
import io.aconite.StringSerializer
import io.aconite.server.adapters.SuspendCallAdapter
import io.aconite.server.errors.PassErrorHandler
import io.aconite.server.filters.PassMethodFilter
import io.aconite.server.serializers.SimpleBodySerializer
import io.aconite.server.serializers.SimpleStringSerializerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType

interface CallAdapter {
    fun adapt(fn: KFunction<*>): KFunction<*>?
}

interface MethodFilter {
    fun predicate(fn: KFunction<*>): Boolean
}

interface ErrorHandler {
    fun handle(ex: Throwable): Response
}

class AconiteServerException(message: String): Exception(message)

class AconiteServer(
        val bodySerializer: BodySerializer.Factory = SimpleBodySerializer.Factory,
        val stringSerializer: StringSerializer.Factory = SimpleStringSerializerFactory(),
        val callAdapter: CallAdapter = SuspendCallAdapter,
        val methodFilter: MethodFilter = PassMethodFilter,
        val errorHandler: ErrorHandler = PassErrorHandler
) {
    private val modules = mutableListOf<RootHandler>()

    fun <T: Any> register(obj: T, iface: KClass<T>) {
        modules.add(RootHandler(this, obj, iface.createType()))
    }

    suspend fun accept(url: String, request: Request): Response? {
        try {
            for (router in modules)
                return router.accept(url, request) ?: continue
            return null
        } catch (ex: Throwable) {
            return errorHandler.handle(ex)
        }
    }
}