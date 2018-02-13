package io.aconite.server

import io.aconite.AconiteException
import io.aconite.Request
import io.aconite.Response
import io.aconite.parser.*
import io.aconite.utils.UrlTemplate
import java.util.*
import kotlin.reflect.KFunction

internal abstract class AbstractHandler : Comparable<AbstractHandler> {
    abstract val requiredArgsCount: Int
    abstract suspend fun accept(obj: Any, url: String, request: Request): Response?
    final override fun compareTo(other: AbstractHandler) = requiredArgsCount.compareTo(other.requiredArgsCount)
}

internal class MethodHandler(server: AconiteServer, desc: HttpMethodDesc) : AbstractHandler() {
    private val method = desc.method
    private val fn = desc.function
    private val args = transformRequestParams(server, desc.arguments)
    private val responseSerializer = responseSerializer(server, desc.function, desc.response)
    override val requiredArgsCount = args.count { !it.isOptional }

    override suspend fun accept(obj: Any, url: String, request: Request): Response? {
        if (url != "/") return null
        if (request.method != method) return null
        val result = fn.httpCall(args, obj, request)
        return responseSerializer(result)
    }
}

internal class ModuleHandler(server: AconiteServer, desc: ModuleMethodDesc): AbstractHandler() {
    private val fn = desc.function
    private val interceptors = server.interceptors
    private val args = transformRequestParams(server, desc.arguments)
    private val routers = buildRouters(server, desc.response)
    override val requiredArgsCount = args.count { !it.isOptional }

    override suspend fun accept(obj: Any, url: String, request: Request): Response? {
        val nextObj = fn.httpCall(args, obj, request)!!
        return processHandlerRouters(interceptors, routers, nextObj, url, request)
    }
}

internal class RootHandler(server: AconiteServer, private val factory: () -> Any, desc: ModuleDesc) {
    private val interceptors = server.interceptors
    private val routers = buildRouters(server, desc)

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

private class MethodToHandler(private val server: AconiteServer) : MethodDesc.Visitor<AbstractHandler> {
    override fun module(desc: ModuleMethodDesc) = ModuleHandler(server, desc)
    override fun http(desc: HttpMethodDesc) = MethodHandler(server, desc)
    override fun webSocket(desc: WebSocketMethodDesc) = throw NotImplementedError()
}

private fun buildRouters(server: AconiteServer, module: ModuleDesc): List<Router> {
    val allHandlers = hashMapOf<UrlTemplate, MutableList<AbstractHandler>>()
    val methodToHandler = MethodToHandler(server)

    for (method in module.methods) {
        if (!server.methodFilter.predicate(method.function)) continue
        val urlHandlers = allHandlers.computeIfAbsent(method.url) { ArrayList() }
        val handler = method.visit(methodToHandler)
        urlHandlers.add(handler)
    }

    return allHandlers
            .map { Router(it.key, it.value.sortedDescending()) }
            .sortedDescending()
}

typealias ResponseSerializer = (Any?) -> Response

private class ResponseToSerializer(
        private val fn: KFunction<*>,
        private val server: AconiteServer
) : ResponseDesc.Visitor<ResponseSerializer> {

    override fun body(desc: BodyResponseDesc): ResponseSerializer {
        val bodySerializer = server.bodySerializer.create(fn, desc.type) ?:
        throw AconiteException("No suitable serializer found for response body of method '$fn'")
        return { r -> Response(body = bodySerializer.serialize(r)) }
    }

    override fun complex(desc: ComplexResponseDesc): ResponseSerializer {
        val transformers = transformResponseParams(server, desc)
        return { r ->
            transformers.map { it.process(r!!) }
                    .reduce { a, b -> a + b }
        }
    }
}

private fun responseSerializer(server: AconiteServer, fn: KFunction<*>, desc: ResponseDesc): ResponseSerializer {
    val responseToSerializer = ResponseToSerializer(fn, server)
    return desc.visit(responseToSerializer)
}