package io.aconite.client

import io.aconite.utils.allOrNull
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaMethod

typealias KotlinInvocationHandler = (KFunction<*>, Array<Any?>) -> Any?

internal object KotlinProxyFactory {
    private val map = ConcurrentHashMap<Class<*>, ProxyInfo>()

    private data class ProxyInfo(
            val ctor: Constructor<*>,
            val methodMap: Map<Method, KFunction<*>>
    )

    private class ModuleProxyHandler(proxyInfo: ProxyInfo, val handler: KotlinInvocationHandler): InvocationHandler {
        val methodMap = proxyInfo.methodMap

        override fun invoke(proxy: Any, method: Method, args: Array<Any?>): Any? {
            val fn = methodMap[method]!!
            return handler(fn, args)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun create(iface: KClass<*>, handler: KotlinInvocationHandler): Any {
        val cls = iface.java
        val info = map.computeIfAbsent(cls) {
            val proxyCls = Proxy.getProxyClass(cls.classLoader, cls)
            ProxyInfo(
                    ctor = proxyCls.getConstructor(InvocationHandler::class.java),
                    methodMap = iface.functions
                            .mapNotNull { Pair(it.javaMethod, it).allOrNull() }
                            .toMap()
            )
        }
        return info.ctor.newInstance(ModuleProxyHandler(info, handler))
    }
}