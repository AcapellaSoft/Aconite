package io.aconite.client

import io.aconite.Request
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap

private class ModuleProxyHandler(val request: Request): InvocationHandler {
    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

internal object ModuleProxyFactory {
    private val map = ConcurrentHashMap<Class<*>, (InvocationHandler) -> Any>()

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> create(cls: Class<T>, request: Request): T {
        val factory = map.computeIfAbsent(cls) {
            val proxyCls = Proxy.getProxyClass(cls.classLoader, cls)
            val ctor = proxyCls.getConstructor(InvocationHandler::class.java);
            { handler: InvocationHandler -> ctor.newInstance(handler) }
        }
        val handler = ModuleProxyHandler(request)
        return factory(handler) as T
    }
}