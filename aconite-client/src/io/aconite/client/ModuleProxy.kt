package io.aconite.client

import io.aconite.Request
import io.aconite.parser.*
import io.aconite.utils.startCoroutine
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.reflect.KFunction
import kotlin.reflect.KType

private class AdaptedFunctionProxy(
        val proxy: FunctionProxy
) {
    @Suppress("UNCHECKED_CAST")
    fun call(url: String, request: Request, args: Array<Any?>): Any? {
        val continuation = args.last() as Continuation<Any?>
        val realArgs = args.sliceArray(0 until args.size - 1)
        return startCoroutine(continuation) { proxy.call(url, request, realArgs) }
    }
}

internal class ModuleProxy(client: AconiteClient, desc: ModuleDesc) {
    private val proxyMap = buildFunctionProxies(client, desc)

    class Factory(val client: AconiteClient) {
        private val map = ConcurrentHashMap<KType, ModuleProxy>()

        fun create(desc: ModuleDesc) = map.computeIfAbsent(desc.type) { ModuleProxy(client, desc) }
    }

    @Suppress("UNCHECKED_CAST")
    fun invoke(fn: KFunction<*>, url: String, request: Request, args: Array<Any?>): Any? {
        val proxy = proxyMap[fn]!!
        return proxy.call(url, request, args)
    }
}

private class MethodToProxy(private val client: AconiteClient) : MethodDesc.Visitor<FunctionProxy> {
    override fun module(desc: ModuleMethodDesc) = FunctionModuleProxy(client, desc)
    override fun http(desc: HttpMethodDesc) = FunctionMethodProxy(client, desc)
    override fun webSocket(desc: WebSocketMethodDesc) = throw NotImplementedError()
}

private fun buildFunctionProxies(client: AconiteClient, desc: ModuleDesc): Map<KFunction<*>, AdaptedFunctionProxy> {
    val methodToProxy = MethodToProxy(client)
    return desc.methods
            .map { Pair(it.originalFunction, AdaptedFunctionProxy(it.visit(methodToProxy))) }
            .toMap()
}