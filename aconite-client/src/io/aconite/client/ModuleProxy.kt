package io.aconite.client

import io.aconite.AconiteException
import io.aconite.Request
import io.aconite.utils.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.functions

private class AdaptedFunctionProxy(
        val proxy: FunctionProxy,
        val adapter: CallAdapter
) {
    fun call(url: String, request: Request, args: Array<Any?>): Any? = adapter.call(args) { adaptedArgs ->
        proxy.call(url, request, adaptedArgs)
    }
}

internal class ModuleProxy(client: AconiteClient, iface: KType) {
    private val proxyMap = buildFunctionProxies(client, iface)

    class Factory(val client: AconiteClient) {
        private val map = ConcurrentHashMap<KType, ModuleProxy>()

        fun create(iface: KType) = map.computeIfAbsent(iface) { ModuleProxy(client, iface) }
    }

    @Suppress("UNCHECKED_CAST")
    fun invoke(fn: KFunction<*>, url: String, request: Request, args: Array<Any?>): Any? {
        val proxy = proxyMap[fn]!!
        return proxy.call(url, request, args)
    }
}

private fun buildFunctionProxies(client: AconiteClient, iface: KType): Map<KFunction<*>, AdaptedFunctionProxy> {
    val cls = iface.cls()
    val proxies = hashMapOf<KFunction<*>, AdaptedFunctionProxy>()

    for (fn in cls.functions) {
        val (url, method) = fn.getHttpMethod() ?: continue
        val resolved = resolve(iface, fn)
        if (resolved.isOpen) continue
        val adapter = adaptFunction(client, resolved)
        val adaptedFn = adapter.function
        val proxy = when (method) {
            null -> FunctionModuleProxy(client, adaptedFn, url)
            else -> FunctionMethodProxy(client, adaptedFn, url, method)
        }
        proxies[fn] = AdaptedFunctionProxy(proxy, adapter)
    }

    return proxies
}

private fun adaptFunction(client: AconiteClient, fn: KFunction<*>): CallAdapter {
    return client.callAdapter.create(fn) ?:
            throw AconiteException("No suitable adapter found for function $fn")
}