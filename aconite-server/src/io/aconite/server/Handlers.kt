package io.aconite.server

import io.aconite.annotations.Body
import io.aconite.annotations.Header
import io.aconite.annotations.Path
import io.aconite.annotations.Query
import kotlinx.coroutines.experimental.future.await
import java.util.concurrent.CompletableFuture
import kotlin.reflect.*
import kotlin.reflect.full.isSubclassOf

private val PARAM_ANNOTATIONS = listOf(
        Body::class,
        Header::class,
        Path::class,
        Query::class
)

abstract class AbstractHandler : Comparable<AbstractHandler> {
    abstract val argsCount: Int
    abstract suspend fun accept(obj: Any, url: String, request: Request): Response?
    final override fun compareTo(other: AbstractHandler) = argsCount.compareTo(other.argsCount)
}

class MethodHandler(server: AconiteServer, private val method: String, private val fn: KFunction<*>) : AbstractHandler() {
    private val args = transformParams(server, fn)
    private val responseSerializer = responseSerializer(server, fn)
    override val argsCount = args.size

    override suspend fun accept(obj: Any, url: String, request: Request): Response? {
        if (request.method != method) return null
        return fn.callBy(args, obj, request)?.await()?.let {
            Response(body = responseSerializer.serialize(it))
        }
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

private fun responseSerializer(server: AconiteServer, fn: KFunction<*>): BodySerializer {
    return server.bodySerializer.create(fn, fn.asyncReturnType()) ?:
            throw AconiteServerException("No suitable serializer found for response body of method $fn")
}

private fun KCallable<*>.asyncReturnType(): KType {
    val cls = this.returnClass()

    if (!CompletableFuture::class.isSubclassOf(cls))
        throw AconiteServerException("Return type of method $this is not CompletableFuture<*>")

    return returnType.arguments[0].type!!
}

private fun KCallable<*>.returnClass(): KClass<*> {
    return returnType.classifier as? KClass<*> ?:
            throw AconiteServerException("Return type of method $this is not determined")
}

suspend private fun KCallable<*>.callBy(args: List<ArgumentTransformer>, obj: Any, request: Request): CompletableFuture<*>? {
    if (!args.all { it.check(request) }) return null
    val values = args
            .mapNotNull { it.process(obj, request) }
            .toMap()
    return callBy(values) as CompletableFuture<*>
}