package io.aconite.client

import io.aconite.*
import io.aconite.serializers.BuildInStringSerializers
import io.aconite.serializers.SimpleBodySerializer
import io.aconite.client.adapters.SuspendCallAdapter
import io.aconite.client.errors.PassErrorHandler
import kotlinx.coroutines.experimental.Unconfined
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType

interface HttpClient {
    suspend fun makeRequest(url: String, request: Request): Response
}

interface CallAdapter {
    interface Factory {
        fun create(fn: KFunction<*>): CallAdapter?
    }

    val function: KFunction<*>
    fun call(args: Array<Any?>, fn: suspend (Array<Any?>) -> Any?): Any?
}

interface ErrorHandler {
    fun handle(error: Response): HttpException
}

class AconiteClient(
        val httpClient: HttpClient,
        val bodySerializer: BodySerializer.Factory = SimpleBodySerializer.Factory,
        val stringSerializer: StringSerializer.Factory = BuildInStringSerializers,
        val callAdapter: CallAdapter.Factory = SuspendCallAdapter.Factory,
        val errorHandler: ErrorHandler = PassErrorHandler,
        val coroutineContext: CoroutineContext = Unconfined
) {
    internal val moduleFactory = ModuleProxy.Factory(this)

    fun <T: Any> create(iface: KClass<T>): T {
        val module = moduleFactory.create(iface.createType())
        return KotlinProxyFactory.create(iface) { fn, args -> module.invoke(fn, "", Request(), args) }
    }

    inline fun <reified T: Any> create() = create(T::class)
}