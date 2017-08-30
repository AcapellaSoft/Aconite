package io.aconite.server

import io.aconite.*
import io.aconite.annotations.Body
import io.aconite.annotations.Header
import io.aconite.annotations.Path
import io.aconite.annotations.Query
import io.aconite.utils.*
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.functions

private val PARAM_ANNOTATIONS = listOf(
        Body::class,
        Header::class,
        Path::class,
        Query::class
)

internal abstract class AbstractHandler : Comparable<AbstractHandler> {
    abstract val argsCount: Int
    abstract suspend fun accept(obj: Any, url: String, request: Request): Response?
    final override fun compareTo(other: AbstractHandler) = argsCount.compareTo(other.argsCount)
}

internal class MethodHandler(server: AconiteServer, private val method: String, private val fn: KFunction<*>) : AbstractHandler() {
    private val args = transformParams(server, fn)
    private val responseSerializer = responseSerializer(server, fn)
    override val argsCount = args.size

    override suspend fun accept(obj: Any, url: String, request: Request): Response? {
        if (url != "/") return null
        if (request.method != method) return null
        val result = fn.httpCall(args, obj, request)
        return Response(body = responseSerializer?.serialize(result).toChannel())
    }
}

internal class ModuleHandler(server: AconiteServer, iface: KType, fn: KFunction<*>): AbstractHandler() {
    private val fn = resolve(iface, fn)
    private val args = transformParams(server, fn)
    private val routers = buildRouters(server, iface)
    override val argsCount = args.size

    override suspend fun accept(obj: Any, url: String, request: Request): Response? {
        val nextObj = fn.httpCall(args, obj, request)
        for (router in routers)
            return router.accept(nextObj!!, url, request) ?: continue
        return null
    }
}

internal class RootHandler(server: AconiteServer, private val obj: Any, iface: KType) {
    private val routers = buildRouters(server, iface)

    suspend fun accept(url: String, request: Request): Response? {
        for (router in routers)
            return router.accept(obj, url, request) ?: continue
        return null
    }
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

private fun transformParams(server: AconiteServer, fn: KCallable<*>): List<ArgumentTransformer> {
    return fn.parameters.map { transformParam(server, it) }
}

private fun transformParam(server: AconiteServer, param: KParameter): ArgumentTransformer {
    if (param.kind == KParameter.Kind.INSTANCE)
        return InstanceTransformer()
    if (param.kind == KParameter.Kind.EXTENSION_RECEIVER)
        throw AconiteException("Extension methods are not allowed")

    val annotations = param.annotations.filter { it.annotationClass in PARAM_ANNOTATIONS }
    if (annotations.isEmpty()) throw AconiteException("Parameter '$param' is not annotated")
    if (annotations.size > 1) throw AconiteException("Parameter '$param' has more than one annotations")
    val annotation = annotations.first()

    return when (annotation) {
        is Body -> BodyTransformer(server, param)
        is Header -> HeaderTransformer(server, param, annotation.name)
        is Path -> PathTransformer(server, param, annotation.name)
        is Query -> QueryTransformer(server, param, annotation.name)
        else -> throw RuntimeException("Unknown annotation $annotation") // should not happen
    }
}

private interface ArgumentTransformer {
    val name: String
    fun check(request: Request): Boolean
    fun process(instance: Any, request: Request): Any?
}

private class BodyTransformer(server: AconiteServer, param: KParameter): ArgumentTransformer {
    private val isNullable = param.type.isMarkedNullable
    private val serializer = server.bodySerializer.create(param, param.type) ?:
            throw AconiteException("No suitable serializer found for body parameter '$param'")

    override val name = param.name!!

    override fun check(request: Request) = isNullable || request.body != null

    override fun process(instance: Any, request: Request): Any? {
        val data = serializer.deserialize(request.body ?: return null)
        return data
    }
}

private class HeaderTransformer(server: AconiteServer, param: KParameter, name: String): ArgumentTransformer {
    private val isNullable = param.type.isMarkedNullable
    private val serializer = server.stringSerializer.create(param, param.type) ?:
            throw AconiteException("No suitable serializer found for header parameter '$param'")

    override val name = if (name.isEmpty()) param.name!! else name

    override fun check(request: Request) = isNullable || request.headers.containsKey(name)

    override fun process(instance: Any, request: Request): Any? {
        val header = request.headers[name] ?: return null
        val data = serializer.deserialize(header)
        return data
    }
}

private class PathTransformer(server: AconiteServer, param: KParameter, name: String): ArgumentTransformer {
    private val isNullable = param.type.isMarkedNullable
    private val serializer = server.stringSerializer.create(param, param.type) ?:
            throw AconiteException("No suitable serializer found for path parameter '$param'")

    override val name = if (name.isEmpty()) param.name!! else name

    override fun check(request: Request) = isNullable || request.path.containsKey(name)

    override fun process(instance: Any, request: Request): Any? {
        val header = request.path[name] ?: return null
        val data = serializer.deserialize(header)
        return data
    }
}

private class QueryTransformer(server: AconiteServer, param: KParameter, name: String): ArgumentTransformer {
    private val isNullable = param.type.isMarkedNullable
    private val serializer = server.stringSerializer.create(param, param.type) ?:
            throw AconiteException("No suitable serializer found for query parameter '$param'")

    override val name = if (name.isEmpty()) param.name!! else name

    override fun check(request: Request) = isNullable || request.query.containsKey(name)

    override fun process(instance: Any, request: Request): Any? {
        val header = request.query[name] ?: return null
        val data = serializer.deserialize(header)
        return data
    }
}

private class InstanceTransformer: ArgumentTransformer {
    override val name = "instance"
    override fun check(request: Request) = true
    override fun process(instance: Any, request: Request) = instance
}

private fun responseSerializer(server: AconiteServer, fn: KFunction<*>): BodySerializer? {
    val returnType = fn.asyncReturnType()
    if (returnType.classifier == Unit::class) return null
    if (returnType.classifier == Void::class) return null

    return server.bodySerializer.create(fn, returnType) ?:
            throw AconiteException("No suitable serializer found for response body of method '$fn'")
}

private suspend fun KFunction<*>.httpCall(args: List<ArgumentTransformer>, obj: Any, request: Request): Any? {
    val missingArgs = args
            .filter { !it.check(request) }
            .map { it.name }
    if (missingArgs.isNotEmpty()) {
        val argsStr = missingArgs.joinToString()
        throw ArgumentMissingException("Missing required arguments: $argsStr")
    }

    val values = args.map { it.process(obj, request) }
    val result = asyncCall(*values.toTypedArray())
    return result
}

private fun adaptFunction(server: AconiteServer, fn: KFunction<*>): KFunction<*> {
    return server.callAdapter.adapt(fn) ?:
            throw AconiteException("No suitable adapter found for function '$fn'")
}