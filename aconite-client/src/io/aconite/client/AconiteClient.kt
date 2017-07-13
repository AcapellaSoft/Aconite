package io.aconite.client

import io.aconite.BodySerializer
import io.aconite.Request
import io.aconite.Response
import io.aconite.StringSerializer
import io.aconite.serializers.BuildInStringSerializers
import io.aconite.serializers.SimpleBodySerializer
import io.aconite.client.adapters.SuspendCallAdapter
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

class AconiteClient(
        val httpClient: HttpClient,
        val bodySerializer: BodySerializer.Factory = SimpleBodySerializer.Factory,
        val stringSerializer: StringSerializer.Factory = BuildInStringSerializers,
        val callAdapter: CallAdapter.Factory = SuspendCallAdapter.Factory
) {
    internal val moduleFactory = ModuleProxy.Factory(this)

    fun <T: Any> create(iface: KClass<T>): T {
        val module = moduleFactory.create(iface.createType())
        return KotlinProxyFactory.create(iface) { fn, args -> module.invoke(fn, "", Request(), args) }
    }

    inline fun <reified T: Any> create() = create(T::class)
}