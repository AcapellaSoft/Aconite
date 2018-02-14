package io.aconite.client

import io.aconite.*
import io.aconite.client.errors.PassErrorHandler
import io.aconite.parser.ModuleParser
import io.aconite.serializers.BuildInStringSerializers
import io.aconite.serializers.SimpleBodySerializer
import kotlin.reflect.KClass

interface HttpClient {
    suspend fun makeRequest(url: String, request: Request): Response
}

interface ErrorHandler {
    fun handle(error: Response): HttpException
}

class AconiteClient(
        val httpClient: HttpClient,
        val bodySerializer: BodySerializer.Factory = SimpleBodySerializer.Factory,
        val stringSerializer: StringSerializer.Factory = BuildInStringSerializers,
        val errorHandler: ErrorHandler = PassErrorHandler
) {
    private val parser = ModuleParser()
    internal val moduleFactory = ModuleProxy.Factory(this)

    fun <T: Any> create(iface: KClass<T>): Service<T> {
        val desc = parser.parse(iface)
        val module = moduleFactory.create(desc)
        return ServiceImpl(module, iface)
    }

    inline fun <reified T: Any> create() = create(T::class)
}