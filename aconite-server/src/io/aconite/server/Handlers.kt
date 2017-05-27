package io.aconite.server

import io.aconite.annotations.Body
import io.aconite.annotations.Header
import io.aconite.annotations.Path
import io.aconite.annotations.Query
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter

private val PARAM_ANNOTATIONS = listOf(
        Body::class,
        Header::class,
        Path::class,
        Query::class
)

abstract class AbstractHandler : Comparable<AbstractHandler> {
    abstract val argsCount: Int
    abstract fun accept(obj: Any, url: String, request: Request): Response?
    final override fun compareTo(other: AbstractHandler) = argsCount.compareTo(other.argsCount)
}

class MethodHandler(server: AconiteServer, private val fn: KCallable<*>) : AbstractHandler() {
    private val args = transformParams(server, fn)
    private val responseSerializer = server.bodySerializer.create(fn, fn.returnType) ?:
            throw AconiteServerException("No suitable serializer found for response body")
    override val argsCount: Int = args.size

    override fun accept(obj: Any, url: String, request: Request): Response? {
        val check = args.all { it.check(request) }
        if (!check) return null

        val values = args
                .mapNotNull { it.process(obj, request) }
                .toMap()

        val result = fn.callBy(values)

        return Response(
                body = responseSerializer.serialize(result)
        )
    }
}

private fun transformParams(server: AconiteServer, fn: KCallable<*>): List<ArgumentTransformer> {
    return fn.parameters.map { transformParam(server, it) }
}

private fun transformParam(server: AconiteServer, param: KParameter): ArgumentTransformer {
    if (param.kind == KParameter.Kind.INSTANCE)
        return InstanceTransformer(param)
    if (param.kind == KParameter.Kind.EXTENSION_RECEIVER)
        throw AconiteServerException("Extension methods are not allowed")

    val annotations = param.annotations.filter { it.annotationClass in PARAM_ANNOTATIONS }
    if (annotations.isEmpty()) throw AconiteServerException("Parameter $param is not annotated")
    if (annotations.size > 1) throw AconiteServerException("Parameter $param has more than one annotations")
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
    fun check(request: Request): Boolean
    fun process(instance: Any, request: Request): Pair<KParameter, Any?>?
}

private class BodyTransformer(server: AconiteServer, private val param: KParameter): ArgumentTransformer {
    private val serializer = server.bodySerializer.create(param, param.type) ?:
            throw AconiteServerException("No suitable serializer found for body parameter $param")

    override fun check(request: Request) = param.isOptional || request.body != null

    override fun process(instance: Any, request: Request): Pair<KParameter, Any?>? {
        val data = serializer.deserialize(request.body ?: return null)
        return Pair(param, data)
    }
}

private class HeaderTransformer(
        server: AconiteServer,
        private val param: KParameter,
        name: String
): ArgumentTransformer {
    private val name = if (name.isEmpty()) param.name!! else name
    private val serializer = server.stringSerializer.create(param, param.type) ?:
            throw AconiteServerException("No suitable serializer found for header parameter $param")

    override fun check(request: Request) = param.isOptional || request.headers.containsKey(name)

    override fun process(instance: Any, request: Request): Pair<KParameter, Any?>? {
        val header = request.headers[name] ?: return null
        val data = serializer.deserialize(header)
        return Pair(param, data)
    }
}

private class PathTransformer(
        server: AconiteServer,
        private val param: KParameter,
        name: String
): ArgumentTransformer {
    private val name = if (name.isEmpty()) param.name!! else name
    private val serializer = server.stringSerializer.create(param, param.type) ?:
            throw AconiteServerException("No suitable serializer found for path parameter $param")

    override fun check(request: Request) = param.isOptional || request.path.containsKey(name)

    override fun process(instance: Any, request: Request): Pair<KParameter, Any?>? {
        val header = request.path[name] ?: return null
        val data = serializer.deserialize(header)
        return Pair(param, data)
    }
}

private class QueryTransformer(
        server: AconiteServer,
        private val param: KParameter,
        name: String
): ArgumentTransformer {
    private val name = if (name.isEmpty()) param.name!! else name
    private val serializer = server.stringSerializer.create(param, param.type) ?:
            throw AconiteServerException("No suitable serializer found for query parameter $param")

    override fun check(request: Request) = param.isOptional || request.query.containsKey(name)

    override fun process(instance: Any, request: Request): Pair<KParameter, Any?>? {
        val header = request.query[name] ?: return null
        val data = serializer.deserialize(header)
        return Pair(param, data)
    }
}

private class InstanceTransformer(private val param: KParameter): ArgumentTransformer {
    override fun check(request: Request) = true
    override fun process(instance: Any, request: Request) = Pair(param, instance)
}