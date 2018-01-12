package io.aconite.client

import io.aconite.AconiteException
import io.aconite.Request
import io.aconite.Response
import io.aconite.annotations.*
import io.aconite.utils.UrlTemplate
import io.aconite.utils.asyncReturnType
import io.aconite.utils.cls
import io.aconite.utils.resolve
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

private val PARAM_ANNOTATIONS = listOf(
        Body::class,
        Header::class,
        Path::class,
        Query::class
)

private val RESPONSE_PARAM_ANNOTATIONS = listOf(
        Body::class,
        Header::class
)

internal interface FunctionProxy {
    suspend fun call(url: String, request: Request, args: Array<Any?>): Any?
}

internal class FunctionModuleProxy(
        val client: AconiteClient,
        fn: KFunction<*>,
        url: String
): FunctionProxy {
    private val appliers = buildAppliers(client, fn)
    private val returnType = fn.asyncReturnType()
    private val returnCls = returnType.cls()
    private val url = UrlTemplate(url)

    override suspend fun call(url: String, request: Request, args: Array<Any?>): Any? {
        val appliedRequest = request.apply(appliers, args)
        val appliedUrl = url + this.url.format(appliedRequest.path)
        val handler = client.moduleFactory.create(returnType)
        val module = KotlinProxyFactory.create(returnCls) { fn, innerArgs ->
            handler.invoke(fn, appliedUrl, appliedRequest, innerArgs)
        }
        return module
    }
}

internal class FunctionMethodProxy(
        private val client: AconiteClient,
        fn: KFunction<*>,
        url: String,
        private val method: String
): FunctionProxy {
    private val appliers = buildAppliers(client, fn)
    private val responseDeserializer = responseDeserializer(client, fn)
    private val url = UrlTemplate(url)

    override suspend fun call(url: String, request: Request, args: Array<Any?>): Any? {
        val appliedRequest = request.apply(appliers, args).copy(method = method)
        val appliedUrl = url + this.url.format(appliedRequest.path)
        val response = client.httpClient.makeRequest(appliedUrl, appliedRequest)

        if (response.code ?: 200 != 200)
            throw client.errorHandler.handle(response)
        return responseDeserializer(response)
    }
}

typealias ResponseDeserializer = (response: Response) -> Any?

private val emptyResponseDeserializer: ResponseDeserializer = { null }

private fun responseDeserializer(client: AconiteClient, fn: KFunction<*>): ResponseDeserializer {
    val returnType = fn.asyncReturnType()
    if (returnType.classifier == Unit::class) return emptyResponseDeserializer
    if (returnType.classifier == Void::class) return emptyResponseDeserializer

    val clazz = returnType.classifier as KClass<*>
    val deserializer: ResponseDeserializer

    if (clazz.findAnnotation<ResponseClass>() != null) {
        if (returnType.isMarkedNullable)
            throw AconiteException("Return type of method '$fn' must not be nullable")
        val constructor = clazz.primaryConstructor ?:
                throw AconiteException("Return type of method '$fn' must have primary constructor")
        val resolvedConstructor = resolve(returnType, constructor)
        val transformers = transformResponseParams(client, resolvedConstructor)
        deserializer = { r ->
            val params = transformers.map { it.process(r) }
            resolvedConstructor.call(*params.toTypedArray())
        }
    } else {
        val bodyDeserializer = client.bodySerializer.create(fn, returnType) ?:
                throw AconiteException("No suitable serializer found for response body of method '$fn'")
        deserializer = { r -> r.body?.let { bodyDeserializer.deserialize(it) } }
    }

    return deserializer
}

private fun buildAppliers(client: AconiteClient, fn: KFunction<*>)
        = fn.parameters.mapNotNull { buildApplier(client, it) }

private fun buildApplier(client: AconiteClient, param: KParameter): ArgumentApplier? {
    if (param.kind == KParameter.Kind.INSTANCE)
        return null
    if (param.kind == KParameter.Kind.EXTENSION_RECEIVER)
        throw AconiteException("Extension methods are not allowed")

    val annotations = param.annotations.filter { it.annotationClass in PARAM_ANNOTATIONS }
    if (annotations.isEmpty()) throw AconiteException("Parameter '$param' is not annotated")
    if (annotations.size > 1) throw AconiteException("Parameter '$param' has more than one annotations")
    val annotation = annotations.first()

    return when (annotation) {
        is Body -> BodyApplier(client, param)
        is Header -> HeaderApplier(client, param, annotation.name)
        is Path -> PathApplier(client, param, annotation.name)
        is Query -> QueryApplier(client, param, annotation.name)
        else -> throw RuntimeException("Unknown annotation $annotation") // should not happen
    }
}

private interface ArgumentApplier {
    fun apply(request: Request, value: Any?): Request
}

private class BodyApplier(client: AconiteClient, param: KParameter): ArgumentApplier {
    private val serializer = client.bodySerializer.create(param, param.type) ?:
            throw AconiteException("No suitable serializer found for body parameter '$param'")

    override fun apply(request: Request, value: Any?): Request {
        val serialized = serializer.serialize(value)
        return request.copy(body = serialized)
    }
}

private class HeaderApplier(client: AconiteClient, param: KParameter, name: String): ArgumentApplier {
    private val serializer = client.stringSerializer.create(param, param.type) ?:
            throw AconiteException("No suitable serializer found for header parameter '$param'")

    val name = if (name.isEmpty()) param.name!! else name

    override fun apply(request: Request, value: Any?): Request {
        val serialized = serializer.serialize(value) ?: return request
        return request.copy(
                headers = request.headers + Pair(name, serialized)
        )
    }
}

private class PathApplier(client: AconiteClient, param: KParameter, name: String): ArgumentApplier {
    private val serializer = client.stringSerializer.create(param, param.type) ?:
            throw AconiteException("No suitable serializer found for path parameter '$param'")

    val name = if (name.isEmpty()) param.name!! else name

    override fun apply(request: Request, value: Any?): Request {
        val serialized = serializer.serialize(value) ?: return request
        return request.copy(
                path = request.path + Pair(name, serialized)
        )
    }
}

private class QueryApplier(client: AconiteClient, param: KParameter, name: String): ArgumentApplier {
    private val serializer = client.stringSerializer.create(param, param.type) ?:
            throw AconiteException("No suitable serializer found for query parameter '$param'")

    val name = if (name.isEmpty()) param.name!! else name

    override fun apply(request: Request, value: Any?): Request {
        val serialized = serializer.serialize(value) ?: return request
        return request.copy(
                query = request.query + Pair(name, serialized)
        )
    }
}

private fun Request.apply(appliers: List<ArgumentApplier>, values: Array<Any?>): Request {
    var appliedRequest = this
    for (i in 0..appliers.size - 1)
        appliedRequest = appliers[i].apply(appliedRequest, values[i])
    return appliedRequest
}

fun transformResponseParams(client: AconiteClient, constructor: KFunction<Any>): List<ResponseTransformer> {
    return constructor.parameters.map { transformResponseParam(client, it) }
}

fun transformResponseParam(client: AconiteClient, param: KParameter): ResponseTransformer {
    val annotations = param.annotations.filter { it.annotationClass in RESPONSE_PARAM_ANNOTATIONS }
    if (annotations.isEmpty()) throw AconiteException("Parameter '$param' is not annotated")
    if (annotations.size > 1) throw AconiteException("Parameter '$param' has more than one annotations")
    val annotation = annotations.first()

    return when (annotation) {
        is Body -> BodyResponseTransformer(client, param)
        is Header -> HeaderResponseTransformer(client, param, annotation.name)
        else -> throw RuntimeException("Unknown annotation $annotation") // should not happen
    }
}

interface ResponseTransformer {
    fun process(response: Response): Any?
}

class BodyResponseTransformer(client: AconiteClient, param: KParameter) : ResponseTransformer {
    private val deserializer = client.bodySerializer.create(param, param.type) ?:
            throw AconiteException("No suitable serializer found for body parameter '$param'")
    private val optional = param.type.isMarkedNullable

    override fun process(response: Response): Any? {
        val body = response.body
        return if (optional && body == null) {
            null
        } else {
            deserializer.deserialize(body!!)
        }
    }
}

class HeaderResponseTransformer(client: AconiteClient, param: KParameter, name: String) : ResponseTransformer {
    private val deserializer = client.stringSerializer.create(param, param.type) ?:
            throw AconiteException("No suitable serializer found for header property '$param'")
    private val name = if (name.isEmpty()) param.name else name
    private val optional = param.type.isMarkedNullable

    override fun process(response: Response): Any? {
        val header = response.headers[name]
        return if (optional && header == null) {
            null
        } else {
            deserializer.deserialize(header!!)
        }
    }
}