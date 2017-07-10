package io.aconite.client

import io.aconite.Request
import io.aconite.Response
import io.aconite.utils.cls
import kotlin.reflect.KFunction

internal interface FunctionProxy {
    suspend fun call(request: Request, args: List<Any?>): Any?
}

internal class FunctionModuleProxy(client: AconiteClient, fn: KFunction<*>): FunctionProxy {
    private val appliers = emptyList<ArgumentApplier>()
    private val returnCls = fn.returnType.cls()

    override suspend fun call(request: Request, args: List<Any?>): Any? {
        val appliedRequest = request.apply(appliers, args)
        val module = ModuleProxyFactory.create(returnCls.java, appliedRequest)
        return module
    }
}

internal class FunctionMethodProxy(val client: AconiteClient, fn: KFunction<*>): FunctionProxy {
    private val appliers = emptyList<ArgumentApplier>()

    override suspend fun call(request: Request, args: List<Any?>): Response {
        val appliedRequest = request.apply(appliers, args)
        return client.httpClient.makeRequest(appliedRequest)
    }
}

internal interface ArgumentApplier {
    fun apply(request: Request, value: Any?): Request
}

private fun Request.apply(appliers: List<ArgumentApplier>, values: List<Any?>): Request {
    var appliedRequest = this
    for (i in 0..appliers.size - 1)
        appliedRequest = appliers[i].apply(appliedRequest, values[i])
    return appliedRequest
}