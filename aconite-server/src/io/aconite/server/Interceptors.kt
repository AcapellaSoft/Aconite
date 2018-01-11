package io.aconite.server

import io.aconite.Request
import io.aconite.Response
import io.aconite.annotations.AfterRequest
import io.aconite.annotations.BeforeRequest
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

internal typealias Interceptor = suspend (obj: Any, url: String, request: Request) -> Unit

internal class Interceptors(private val server: AconiteServer) {
    private val beforeMap = ConcurrentHashMap<Class<*>, Interceptor>()
    private val afterMap = ConcurrentHashMap<Class<*>, Interceptor>()

    suspend fun beforeRequest(obj: Any, url: String, request: Request) {
        val before = beforeMap.computeIfAbsent(obj.javaClass) { buildBefore(obj::class) }
        before(obj, url, request)
    }

    suspend fun afterRequest(obj: Any, url: String, request: Request) {
        val after = afterMap.computeIfAbsent(obj.javaClass) { buildAfter(obj::class) }
        after(obj, url, request)
    }

    private fun buildBefore(clazz: KClass<*>) = buildInterceptFunction<BeforeRequest>(clazz)

    private fun buildAfter(clazz: KClass<*>) = buildInterceptFunction<AfterRequest>(clazz)

    private inline fun <reified T : Annotation> buildInterceptFunction(clazz: KClass<*>): Interceptor {
        val interceptFunctions = clazz.functions
                .filter { it.findAnnotation<T>() != null }
                .map { InterceptWrapper(server, it) }

        return { obj, _, request ->
            for (fn in interceptFunctions) fn(obj, request)
        }
    }
}

internal class InterceptWrapper(server: AconiteServer, private val fn: KFunction<*>) {
    private val args = transformRequestParams(server, fn)

    suspend operator fun invoke(obj: Any, request: Request) {
        val response = CoroutineResponseReference(Response()) // todo: remove this
        fn.httpCall(args, obj, request, response)
    }
}