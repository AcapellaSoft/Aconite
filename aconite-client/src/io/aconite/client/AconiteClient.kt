package io.aconite.client

import io.aconite.BodySerializer
import io.aconite.RequestAcceptor
import io.aconite.StringSerializer
import io.aconite.parser.ModuleParser
import io.aconite.serializers.BuildInStringSerializers
import io.aconite.serializers.SimpleBodySerializer
import kotlin.reflect.KClass

class AconiteClient(
        val acceptor: RequestAcceptor,
        val bodySerializer: BodySerializer.Factory = SimpleBodySerializer.Factory,
        val stringSerializer: StringSerializer.Factory = BuildInStringSerializers
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