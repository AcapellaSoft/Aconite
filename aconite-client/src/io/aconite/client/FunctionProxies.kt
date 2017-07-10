package io.aconite.client

import io.aconite.AconiteException
import io.aconite.Request
import io.aconite.Response
import io.aconite.annotations.Body
import io.aconite.annotations.Header
import io.aconite.annotations.Path
import io.aconite.annotations.Query
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
    suspend fun call(request: Request, args: List<Any?>): Any?
}

internal class FunctionModuleProxy(client: AconiteClient, fn: KFunction<*>): FunctionProxy {
    private val appliers = buildAppliers(client, fn)
    private val returnCls = fn.returnType.cls()

    override suspend fun call(request: Request, args: List<Any?>): Any? {
        val appliedRequest = request.apply(appliers, args)
        val module = ModuleProxyFactory.create(returnCls.java, appliedRequest)
        return module
    }
}

internal class FunctionMethodProxy(client: AconiteClient, fn: KFunction<*>): FunctionProxy {
    private val appliers = buildAppliers(client, fn)
    private val client = client.httpClient

    override suspend fun call(request: Request, args: List<Any?>): Response {
        val appliedRequest = request.apply(appliers, args)
        return client.makeRequest(appliedRequest)
    }
}

private fun buildAppliers(client: AconiteClient, fn: KFunction<*>)
        = fn.parameters.mapNotNull { buildApplier(client, it) }

private fun buildApplier(client: AconiteClient, param: KParameter): ArgumentApplier? {
    if (param.kind == KParameter.Kind.INSTANCE)
        return null
    if (param.kind == KParameter.Kind.EXTENSION_RECEIVER)
        throw AconiteException("Extension methods are not allowed")

    val annotations = param.annotations.filter { it.annotationClass in PARAM_ANNOTATIONS }
    if (annotations.isEmpty()) throw AconiteException("Parameter $param is not annotated")
    if (annotations.size > 1) throw AconiteException("Parameter $param has more than one annotations")
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
            throw AconiteException("No suitable serializer found for body parameter $param")

    override fun apply(request: Request, value: Any?): Request {
        val serialized = serializer.serialize(value)
        return request.copy(body = serialized)
    }
}

private class HeaderApplier(client: AconiteClient, param: KParameter, name: String): ArgumentApplier {
    private val serializer = client.stringSerializer.create(param, param.type) ?:
            throw AconiteException("No suitable serializer found for header parameter $param")

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
            throw AconiteException("No suitable serializer found for path parameter $param")

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
            throw AconiteException("No suitable serializer found for query parameter $param")

    val name = if (name.isEmpty()) param.name!! else name

    override fun apply(request: Request, value: Any?): Request {
        val serialized = serializer.serialize(value) ?: return request
        return request.copy(
                query = request.query + Pair(name, serialized)
        )
    }
}

private fun Request.apply(appliers: List<ArgumentApplier>, values: List<Any?>): Request {
    var appliedRequest = this
    for (i in 0..appliers.size - 1)
        appliedRequest = appliers[i].apply(appliedRequest, values[i])
    return appliedRequest
}