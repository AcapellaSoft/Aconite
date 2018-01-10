package io.aconite.server

import io.aconite.AconiteException
import io.aconite.ArgumentMissingException
import io.aconite.Request
import io.aconite.annotations.Body
import io.aconite.annotations.Header
import io.aconite.annotations.Path
import io.aconite.annotations.Query
import io.aconite.utils.asyncCall
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

private val PARAM_ANNOTATIONS = listOf(
        Body::class,
        Header::class,
        Path::class,
        Query::class
)

internal fun transformParams(server: AconiteServer, fn: KCallable<*>): List<ArgumentTransformer> {
    return fn.parameters.map { transformParam(server, it) }
}

internal fun transformParam(server: AconiteServer, param: KParameter): ArgumentTransformer {
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

internal interface ArgumentTransformer {
    val isNullable: Boolean
    val name: String
    fun check(request: Request): Boolean
    fun process(instance: Any, request: Request): Any?
}

private class BodyTransformer(server: AconiteServer, param: KParameter) : ArgumentTransformer {
    override val isNullable = param.type.isMarkedNullable
    private val serializer = server.bodySerializer.create(param, param.type) ?:
            throw AconiteException("No suitable serializer found for body parameter '$param'")

    override val name = param.name!!

    override fun check(request: Request) = isNullable || request.body != null

    override fun process(instance: Any, request: Request): Any? {
        val data = serializer.deserialize(request.body ?: return null)
        return data
    }
}

private class HeaderTransformer(server: AconiteServer, param: KParameter, name: String) : ArgumentTransformer {
    override val isNullable = param.type.isMarkedNullable
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

private class PathTransformer(server: AconiteServer, param: KParameter, name: String) : ArgumentTransformer {
    override val isNullable = param.type.isMarkedNullable
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

private class QueryTransformer(server: AconiteServer, param: KParameter, name: String) : ArgumentTransformer {
    override val isNullable = param.type.isMarkedNullable
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

private class InstanceTransformer : ArgumentTransformer {
    override val isNullable = false
    override val name = "instance"
    override fun check(request: Request) = true
    override fun process(instance: Any, request: Request) = instance
}

internal suspend fun KFunction<*>.httpCall(
        args: List<ArgumentTransformer>,
        obj: Any,
        request: Request,
        response: CoroutineResponseReference
): Any? {
    val missingArgs = args
            .filter { !it.check(request) }
            .map { it.name }
    if (missingArgs.isNotEmpty()) {
        val argsStr = missingArgs.joinToString()
        throw ArgumentMissingException("Missing required arguments: $argsStr")
    }

    val values = args.map { it.process(obj, request) }
    val result = asyncCall(response, *values.toTypedArray())
    return result
}

internal fun adaptFunction(server: AconiteServer, fn: KFunction<*>): KFunction<*> {
    return server.callAdapter.adapt(fn) ?:
            throw AconiteException("No suitable adapter found for function '$fn'")
}