package io.aconite.client

import io.aconite.Request
import io.aconite.utils.cls
import kotlin.reflect.KFunction

internal interface FunctionProxy {
    suspend fun call(request: Request, args: List<Any?>): Any?
}

internal class FunctionModuleProxy(client: AconiteClient, fn: KFunction<*>): FunctionProxy {
    private val appliers = emptyList<ArgumentApplier>()
    private val returnCls = fn.returnType.cls()

    override suspend fun call(request: Request, args: List<Any?>): Any? {
        var appliedRequest = request
        for (i in 0..appliers.size - 1) appliedRequest = appliers[i].apply(appliedRequest, args[i])
        val module = ModuleProxyFactory.create(returnCls.java, appliedRequest)
        return module
    }
}

internal interface ArgumentApplier {
    fun apply(request: Request, value: Any?): Request
}