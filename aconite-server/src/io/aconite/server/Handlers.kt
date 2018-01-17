package io.aconite.server

import io.aconite.AconiteException
import io.aconite.Request
import io.aconite.Response
import io.aconite.annotations.ResponseClass
import io.aconite.utils.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

internal abstract class AbstractHandler : Comparable<AbstractHandler> {
    abstract val requiredArgsCount: Int
    abstract suspend fun accept(obj: Any, url: String, request: Request): Response?
    final override fun compareTo(other: AbstractHandler) = requiredArgsCount.compareTo(other.requiredArgsCount)
}

internal class MethodHandler(server: AconiteServer, private val method: String, private val fn: KFunction<*>) : AbstractHandler() {
    private val args = transformRequestParams(server, fn)
    private val responseSerializer = responseSerializer(server, fn)
    override val requiredArgsCount = args.count { !it.isNullable }

    override suspend fun accept(obj: Any, url: String, request: Request): Response? {
        if (url != "/") return null
        if (request.method != method) return null
        val result = fn.httpCall(args, obj, request)
        return responseSerializer(result)
    }
}

internal class ModuleHandler(server: AconiteServer, iface: KType, fn: KFunction<*>): AbstractHandler() {
    private val interceptors = server.interceptors
    private val fn = resolve(iface, fn)
    private val args = transformRequestParams(server, fn)
    private val routers = buildRouters(server, iface)
    override val requiredArgsCount = args.count { !it.isNullable }

    override suspend fun accept(obj: Any, url: String, request: Request): Response? {
        val nextObj = fn.httpCall(args, obj, request)!!
        return processHandlerRouters(interceptors, routers, nextObj, url, request)
    }
}

internal class RootHandler(server: AconiteServer, private val factory: () -> Any, iface: KType) {
    private val interceptors = server.interceptors
    private val routers = buildRouters(server, iface)

    suspend fun accept(url: String, request: Request): Response? {
        val obj = factory()
        return processHandlerRouters(interceptors, routers, obj, url, request)
    }
}

private suspend fun processHandlerRouters(
        interceptors: Interceptors,
        routers: List<Router>,
        obj: Any,
        url: String,
        request: Request
): Response? {
    interceptors.beforeRequest(obj, url, request)

    val iter = routers.iterator()
    var result: Response? = null

    while (result == null && iter.hasNext()) {
        val router = iter.next()
        result = router.accept(obj, url, request)
    }

    interceptors.afterRequest(obj, url, request)
    return result
}

private fun buildRouters(server: AconiteServer, iface: KType): List<Router> {
    val cls = iface.cls()
    val allHandlers = hashMapOf<String, MutableList<AbstractHandler>>()

    for (fn in cls.functions) {
        val (url, method) = fn.getHttpMethod() ?: continue
        val resolved = resolve(iface, fn)
        if (!server.methodFilter.predicate(resolved)) continue
        val adapted = adaptFunction(server, resolved)
        val urlHandlers = allHandlers.computeIfAbsent(url) { ArrayList() }
        val handler = when (method) {
            null -> ModuleHandler(server, adapted.asyncReturnType(), adapted)
            else -> MethodHandler(server, method, adapted)
        }
        urlHandlers.add(handler)
    }

    return allHandlers
            .map { Router(UrlTemplate(it.key), it.value.sorted().reversed()) }
            .sorted()
            .reversed()
}

typealias ResponseSerializer = (Any?) -> Response

private val emptyResponseSerializer: ResponseSerializer = { Response() }

private fun responseSerializer(server: AconiteServer, fn: KFunction<*>): ResponseSerializer {
    val returnType = fn.asyncReturnType()
    if (returnType.classifier == Unit::class) return emptyResponseSerializer
    if (returnType.classifier == Void::class) return emptyResponseSerializer

    val clazz = returnType.classifier as KClass<*>
    val serializer: ResponseSerializer

    if (clazz.findAnnotation<ResponseClass>() != null) {
        if (returnType.isMarkedNullable)
            throw AconiteException("Return type of method '$fn' must not be nullable")
        val transformers = transformResponseParams(server, returnType)
        serializer = { r ->
            transformers.map { it.process(r!!) }
                    .reduce { a, b -> a + b }
        }
    } else {
        val bodySerializer = server.bodySerializer.create(fn, returnType) ?:
                throw AconiteException("No suitable serializer found for response body of method '$fn'")
        serializer = { r -> Response(body = bodySerializer.serialize(r)) }
    }

    return serializer
}