package io.aconite.client

import io.aconite.Request
import io.aconite.utils.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.functions

internal class ModuleProxy(client: AconiteClient, iface: KType) {
    private val proxyMap = buildFunctionProxies(client, iface)

    class Factory(val client: AconiteClient) {
        private val map = ConcurrentHashMap<KType, ModuleProxy>()

        fun create(iface: KType) = map.computeIfAbsent(iface) { ModuleProxy(client, iface) }
    }

    @Suppress("UNCHECKED_CAST")
    fun invoke(fn: KFunction<*>, url: String, request: Request, args: Array<Any?>): Any? {
        val proxy = proxyMap[fn]!!
        val continuation = args.last() as Continuation<Any?>
        val realArgs = args.sliceArray(0..args.size - 1)
        return startCoroutine(continuation) { proxy.call(url, request, realArgs) }
    }
}

private fun buildFunctionProxies(client: AconiteClient, iface: KType): Map<KFunction<*>, FunctionProxy> {
    val cls = iface.cls()
    val proxies = hashMapOf<KFunction<*>, FunctionProxy>()

    for (fn in cls.functions) {
        val resolved = resolve(iface, fn)
        if (resolved.isOpen) continue // FIXME: simple solution for filter out functions from 'Any' class
        //val adapted = adaptFunction(server, resolved)
        val adapted = resolved
        val (url, method) = adapted.getHttpMethod()
        val proxy = when (method) {
            null -> FunctionModuleProxy(client, adapted, url)
            else -> FunctionMethodProxy(client, adapted, url, method)
        }
        proxies[fn] = proxy
    }

    return proxies
}