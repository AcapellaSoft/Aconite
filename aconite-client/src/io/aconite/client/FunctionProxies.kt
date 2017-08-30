package io.aconite.client

import io.aconite.AconiteException
import io.aconite.BodySerializer
import io.aconite.Request
import io.aconite.annotations.Body
import io.aconite.annotations.Header
import io.aconite.annotations.Path
import io.aconite.annotations.Query
import io.aconite.utils.UrlTemplate
import io.aconite.utils.asyncReturnType
import io.aconite.utils.cls
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

private val PARAM_ANNOTATIONS = listOf(
        Body::class,
        Header::class,
        Path::class,
        Query::class
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
        val client: AconiteClient,
        fn: KFunction<*>,
        url: String,
        val method: String
): FunctionProxy {
    private val appliers = buildAppliers(client, fn)
    private val responseDeserializer = responseDeserializer(client, fn)
    private val url = UrlTemplate(url)

    override suspend fun call(url: String, request: Request, args: Array<Any?>): Any? {
        val appliedRequest = request.apply(appliers, args).copy(method = method)
        val appliedUrl = url + this.url.format(appliedRequest.path)
        val response = client.httpClient.makeRequest(appliedUrl, appliedRequest)

        if (response.code != 200)
            throw client.errorHandler.handle(response)
        // TODO: channel support
        return response.body.receive().let { responseDeserializer?.deserialize(it) }
    }
}

private fun responseDeserializer(client: AconiteClient, fn: KFunction<*>) : BodySerializer? {
    val returnType = fn.asyncReturnType()
    if (returnType.classifier == Unit::class) return null
    if (returnType.classifier == Void::class) return null

    return client.bodySerializer.create(fn, returnType) ?:
            throw AconiteException("No suitable serializer found for response body in function '$fn'")
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