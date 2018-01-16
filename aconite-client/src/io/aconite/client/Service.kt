package io.aconite.client

import io.aconite.Request
import io.aconite.serializers.Cookie
import io.aconite.serializers.CookieStringSerializer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

interface Service<out T: Any> {
    /**
     * Get or create client proxy for specified [address].
     * @param[address] address in format: http://localhost:8080
     * @return client proxy
     */
    operator fun get(address: String): T

    /**
     * Merge [cookie] with internal cookie map.
     * All subsequent requests will contain this cookie.
     */
    fun setCookie(cookie: Cookie)

    /**
     * Clears internal cookie map.
     * All subsequent requests will contain no cookie.
     */
    fun clearCookie()
}

internal class ServiceImpl<out T: Any>(
        private val module: ModuleProxy,
        private val iface: KClass<T>
) : Service<T> {
    private val proxies = ConcurrentHashMap<String, T>()
    private val cookie = AtomicReference(emptyMap<String, String>())
    private val cookieSerializer = CookieStringSerializer.create(this::get, Cookie::class.createType())!!

    override fun get(address: String): T = proxies.computeIfAbsent(address) {
        // todo: check address format

        val headers = cookie.get().let {
            if (it.isNotEmpty()) mapOf(
                    "Cookie" to cookieSerializer.serialize(Cookie(it))!!
            ) else emptyMap()
        }

        val request = Request(
                headers = headers
        )
        KotlinProxyFactory.create(iface) { fn, args -> module.invoke(fn, address, request, args) }
    }

    override fun setCookie(cookie: Cookie) {
        this.cookie.getAndUpdate {
            it + cookie.data
        }
    }

    override fun clearCookie() {
        this.cookie.set(emptyMap())
    }
}