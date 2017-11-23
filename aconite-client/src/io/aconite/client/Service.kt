package io.aconite.client

import io.aconite.Request
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

interface Service<out T: Any> {
    /**
     * Get or create client proxy for specified [address].
     * @param[address] address in format: http://localhost:8080
     * @return client proxy
     */
    operator fun get(address: String): T
}

internal class ServiceImpl<out T: Any>(
        private val module: ModuleProxy,
        private val iface: KClass<T>
) : Service<T> {
    private val proxies = ConcurrentHashMap<String, T>()

    override fun get(address: String): T = proxies.computeIfAbsent(address) {
        // todo: check address format
        KotlinProxyFactory.create(iface) { fn, args -> module.invoke(fn, address, Request(), args) }
    }
}