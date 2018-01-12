package io.aconite.server

import io.aconite.AconiteException
import io.aconite.ArgumentMissingException
import io.aconite.Request
import io.aconite.Response
import io.aconite.annotations.Body
import io.aconite.annotations.Header
import io.aconite.annotations.Path
import io.aconite.annotations.Query
import io.aconite.utils.asyncCall
import io.aconite.utils.resolve
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

private val REQUEST_PARAM_ANNOTATIONS = listOf(
        Body::class,
        Header::class,
        Path::class,
        Query::class
)

private val RESPONSE_PARAM_ANNOTATIONS = listOf(
        Body::class,
        Header::class
)

internal fun transformRequestParams(server: AconiteServer, fn: KCallable<*>): List<RequestTransformer> {
    return fn.parameters.map { transformRequestParam(server, it) }
}

internal fun transformRequestParam(server: AconiteServer, param: KParameter): RequestTransformer {
    if (param.kind == KParameter.Kind.INSTANCE)
        return InstanceRequestTransformer()
    if (param.kind == KParameter.Kind.EXTENSION_RECEIVER)
        throw AconiteException("Extension methods are not allowed")

    val annotations = param.annotations.filter { it.annotationClass in REQUEST_PARAM_ANNOTATIONS }
    if (annotations.isEmpty()) throw AconiteException("Parameter '$param' is not annotated")
    if (annotations.size > 1) throw AconiteException("Parameter '$param' has more than one annotations")
    val annotation = annotations.first()

    return when (annotation) {
        is Body -> BodyRequestTransformer(server, param)
        is Header -> HeaderRequestTransformer(server, param, annotation.name)
        is Path -> PathRequestTransformer(server, param, annotation.name)
        is Query -> QueryRequestTransformer(server, param, annotation.name)
        else -> throw RuntimeException("Unknown annotation $annotation") // should not happen
    }
}

internal interface RequestTransformer {
    val isNullable: Boolean
    val name: String
    fun check(request: Request): Boolean
    fun process(instance: Any, request: Request): Any?
}

private class BodyRequestTransformer(server: AconiteServer, param: KParameter) : RequestTransformer {
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

private class HeaderRequestTransformer(server: AconiteServer, param: KParameter, name: String) : RequestTransformer {
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

private class PathRequestTransformer(server: AconiteServer, param: KParameter, name: String) : RequestTransformer {
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

private class QueryRequestTransformer(server: AconiteServer, param: KParameter, name: String) : RequestTransformer {
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

private class InstanceRequestTransformer : RequestTransformer {
    override val isNullable = false
    override val name = "instance"
    override fun check(request: Request) = true
    override fun process(instance: Any, request: Request) = instance
}

internal fun transformResponseParams(server: AconiteServer, returnType: KType): List<ResponseTransformer> {
    val clazz = returnType.classifier as KClass<*>
    val constructor = clazz.primaryConstructor
            ?: throw AconiteException("Response class '$clazz' must have primary constructor")

    return constructor.parameters.map { transformResponseParam(server, returnType, it) }
}

internal fun transformResponseParam(server: AconiteServer, returnType: KType, param: KParameter): ResponseTransformer {
    val annotations = param.annotations.filter { it.annotationClass in RESPONSE_PARAM_ANNOTATIONS }
    if (annotations.isEmpty()) throw AconiteException("Parameter '$param' is not annotated")
    if (annotations.size > 1) throw AconiteException("Parameter '$param' has more than one annotations")
    val annotation = annotations.first()

    val clazz = returnType.classifier as KClass<*>
    val property = clazz.memberProperties
            .find { it.name == param.name }
            ?: throw AconiteException("All parameters of response class '$clazz' must be a properties")
    val resolvedProperty = resolve(returnType, property)

    return when (annotation) {
        is Body -> BodyResponseTransformer(server, resolvedProperty)
        is Header -> HeaderResponseTransformer(server, resolvedProperty, annotation.name)
        else -> throw RuntimeException("Unknown annotation $annotation") // should not happen
    }
}

internal interface ResponseTransformer {
    fun process(result: Any): Response
}

private class BodyResponseTransformer(server: AconiteServer, property: KProperty<*>) : ResponseTransformer {
    private val getter = property.getter
    private val serializer = server.bodySerializer.create(property, property.returnType) ?:
            throw AconiteException("No suitable serializer found for body property '$property'")

    override fun process(result: Any): Response {
        val value = getter.call(result)
        val data = serializer.serialize(value)
        return Response(body = data)
    }
}

private class HeaderResponseTransformer(server: AconiteServer, property: KProperty<*>, name: String) : ResponseTransformer {
    private val getter = property.getter
    private val serializer = server.stringSerializer.create(property, property.returnType) ?:
            throw AconiteException("No suitable serializer found for header property '$property'")
    private val name = if (name.isEmpty()) property.name else name

    override fun process(result: Any): Response {
        val value = getter.call(result)
        val data = serializer.serialize(value) ?: return Response()
        return Response(headers = mapOf(name to data))
    }
}

internal suspend fun KFunction<*>.httpCall(
        args: List<RequestTransformer>,
        obj: Any,
        request: Request
): Any? {
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

internal fun adaptFunction(server: AconiteServer, fn: KFunction<*>): KFunction<*> {
    return server.callAdapter.adapt(fn) ?:
            throw AconiteException("No suitable adapter found for function '$fn'")
}