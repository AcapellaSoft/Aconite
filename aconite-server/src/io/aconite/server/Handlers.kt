package io.aconite.server

import io.aconite.annotations.*
import io.aconite.utils.UrlTemplate
import kotlinx.coroutines.experimental.future.await
import java.util.concurrent.CompletableFuture
import kotlin.reflect.*
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubclassOf

private val PARAM_ANNOTATIONS = listOf(
        Body::class,
        Header::class,
        Path::class,
        Query::class
)

private val METHOD_ANNOTATION = listOf(
        DELETE::class,
        GET::class,
        HEAD::class,
        OPTIONS::class,
        PATCH::class,
        POST::class,
        PUT::class
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

class ModuleHandler(server: AconiteServer, iface: KType, private val fn: KFunction<*>): AbstractHandler() {
    private val args = transformParams(server, fn)
    private val routers = buildRouters(server, iface)
    override val argsCount = args.size

    override suspend fun accept(obj: Any, url: String, request: Request): Response? {
        return fn.callBy(args, obj, request)?.await()?.let { nextObj ->
            for (router in routers) {
                val response = router.accept(nextObj, url, request)
                if (response != null) return response
            }
            return null
        }
    }
}

private fun buildRouters(server: AconiteServer, iface: KType): List<AbstractRouter> {
    val cls = iface.cls()
    val allHandlers = hashMapOf<String, MutableList<AbstractHandler>>()

    for (fn in cls.functions) {
        if (!server.methodFilter.predicate(fn)) continue
        val adapted = adaptFunction(server, fn)
        val (url, method) = adapted.getHttpMethod()
        val urlHandlers = allHandlers.computeIfAbsent(url) { ArrayList() }
        val handler = when (method) {
            null -> ModuleHandler(server, adapted.asyncReturnType(), adapted)
            else -> MethodHandler(server, method, fn)
        }
        urlHandlers.add(handler)
    }

    return allHandlers.map { ModuleRouter(UrlTemplate(it.key), it.value) }
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

private fun KType.cls(): KClass<*> {
    return classifier as? KClass<*> ?:
            throw AconiteServerException("Class of $this is not determined")
}

suspend private fun KCallable<*>.callBy(args: List<ArgumentTransformer>, obj: Any, request: Request): CompletableFuture<*>? {
    if (!args.all { it.check(request) }) return null
    val values = args
            .mapNotNull { it.process(obj, request) }
            .toMap()
    return callBy(values) as CompletableFuture<*>
}

private fun adaptFunction(server: AconiteServer, fn: KFunction<*>): KFunction<*> {
    return server.callAdapter.adapt(fn) ?:
            throw AconiteServerException("No suitable adapter found for function $fn")
}

private fun KFunction<*>.getHttpMethod(): Pair<String, String?> {
    val annotations = annotations.filter { it.annotationClass in METHOD_ANNOTATION }
    if (annotations.isEmpty()) throw AconiteServerException("Method $this is not annotated")
    if (annotations.size > 1) throw AconiteServerException("Method $this has more than one annotations")
    val annotation = annotations.first()

    return when (annotation) {
        is HTTP -> Pair(annotation.url, annotation.method)
        is MODULE -> Pair(annotation.value, null)
        else -> {
            val getUrl = annotation.javaClass.getMethod("value")
            Pair(getUrl.invoke(annotation) as String, annotation.annotationClass.simpleName)
        }
    }
}