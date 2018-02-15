package io.aconite.client

import io.aconite.AconiteException
import io.aconite.Request
import io.aconite.Response
import io.aconite.parser.*
import kotlin.reflect.KFunction

internal interface FunctionProxy {
    suspend fun call(url: String, request: Request, args: Array<Any?>): Any?
}

internal class FunctionModuleProxy(
        val client: AconiteClient,
        desc: ModuleMethodDesc
): FunctionProxy {
    private val appliers = buildAppliers(client, desc.arguments)
    private val response = desc.response
    private val returnCls = desc.response.clazz
    private val url = desc.url

    override suspend fun call(url: String, request: Request, args: Array<Any?>): Any? {
        val appliedRequest = request.apply(appliers, args)
        val appliedUrl = url + this.url.format(appliedRequest.path)
        val handler = client.moduleFactory.create(response)
        val module = KotlinProxyFactory.create(returnCls) { fn, innerArgs ->
            handler.invoke(fn, appliedUrl, appliedRequest, innerArgs)
        }
        return module
    }
}

internal class FunctionMethodProxy(
        private val client: AconiteClient,
        desc: HttpMethodDesc
): FunctionProxy {
    private val appliers = buildAppliers(client, desc.arguments)
    private val responseDeserializer = responseDeserializer(client, desc)
    private val url = desc.url
    private val method = desc.method

    override suspend fun call(url: String, request: Request, args: Array<Any?>): Any? {
        val appliedRequest = request.apply(appliers, args).copy(method = method)
        val appliedUrl = url + this.url.format(appliedRequest.path)
        val response = client.acceptor.accept(appliedUrl, appliedRequest)
        return responseDeserializer(response)
    }
}

typealias ResponseDeserializer = (response: Response) -> Any?

private class ResponseToDeserializer(
        private val client: AconiteClient,
        private val fn: KFunction<*>
) : ResponseDesc.Visitor<ResponseDeserializer> {

    override fun body(desc: BodyResponseDesc): ResponseDeserializer {
        val bodyDeserializer = client.bodySerializer.create(fn, desc.type) ?:
                throw AconiteException("No suitable serializer found for response body of method '$fn'")
        return { r -> r.body?.let { bodyDeserializer.deserialize(it) } }
    }

    override fun complex(desc: ComplexResponseDesc): ResponseDeserializer {
        val transformers = transformResponseParams(client, desc.fields)
        return { r ->
            val params = transformers.map { it.process(r) }
            desc.constructor.call(*params.toTypedArray())
        }
    }
}

private fun responseDeserializer(client: AconiteClient, desc: HttpMethodDesc): ResponseDeserializer {
    val responseToDeserializer = ResponseToDeserializer(client, desc.resolvedFunction)
    return desc.response.visit(responseToDeserializer)
}

private class ArgumentToApplier(private val client: AconiteClient) : ArgumentDesc.Visitor<ArgumentApplier> {
    override fun header(desc: HeaderArgumentDesc) = HeaderApplier(client, desc)
    override fun path(desc: PathArgumentDesc) = PathApplier(client, desc)
    override fun query(desc: QueryArgumentDesc) = QueryApplier(client, desc)
    override fun body(desc: BodyArgumentDesc) = BodyApplier(client, desc)
}

private fun buildAppliers(client: AconiteClient, arguments: List<ArgumentDesc>) : List<ArgumentApplier> {
    val argumentToApplier = ArgumentToApplier(client)
    return arguments.map { it.visit(argumentToApplier) }
}

private interface ArgumentApplier {
    fun apply(request: Request, value: Any?): Request
}

private class BodyApplier(client: AconiteClient, desc: BodyArgumentDesc): ArgumentApplier {
    private val serializer = client.bodySerializer.create(desc.parameter, desc.parameter.type) ?:
            throw AconiteException("No suitable serializer found for body parameter '${desc.parameter}'")

    override fun apply(request: Request, value: Any?): Request {
        val serialized = serializer.serialize(value)
        return request.copy(body = serialized)
    }
}

private class HeaderApplier(client: AconiteClient, desc: HeaderArgumentDesc): ArgumentApplier {
    private val serializer = client.stringSerializer.create(desc.parameter, desc.parameter.type) ?:
            throw AconiteException("No suitable serializer found for header parameter '${desc.parameter}'")

    val name = desc.name

    override fun apply(request: Request, value: Any?): Request {
        val serialized = serializer.serialize(value) ?: return request
        return request.copy(
                headers = request.headers + Pair(name, serialized)
        )
    }
}

private class PathApplier(client: AconiteClient, desc: PathArgumentDesc): ArgumentApplier {
    private val serializer = client.stringSerializer.create(desc.parameter, desc.parameter.type) ?:
            throw AconiteException("No suitable serializer found for path parameter '${desc.parameter}'")

    val name = desc.name

    override fun apply(request: Request, value: Any?): Request {
        val serialized = serializer.serialize(value) ?: return request
        return request.copy(
                path = request.path + Pair(name, serialized)
        )
    }
}

private class QueryApplier(client: AconiteClient, desc: QueryArgumentDesc): ArgumentApplier {
    private val serializer = client.stringSerializer.create(desc.parameter, desc.parameter.type) ?:
            throw AconiteException("No suitable serializer found for query parameter '${desc.parameter}'")

    val name = desc.name

    override fun apply(request: Request, value: Any?): Request {
        val serialized = serializer.serialize(value) ?: return request
        return request.copy(
                query = request.query + Pair(name, serialized)
        )
    }
}

private fun Request.apply(appliers: List<ArgumentApplier>, values: Array<Any?>): Request {
    var appliedRequest = this
    for (i in 0 until appliers.size)
        appliedRequest = appliers[i].apply(appliedRequest, values[i])
    return appliedRequest
}

private class FieldToTransformer(private val client: AconiteClient) : FieldDesc.Visitor<ResponseTransformer> {
    override fun header(desc: HeaderFieldDesc) = HeaderResponseTransformer(client, desc)
    override fun body(desc: BodyFieldDesc) = BodyResponseTransformer(client, desc)
}

fun transformResponseParams(client: AconiteClient, fields: List<FieldDesc>): List<ResponseTransformer> {
    val fieldToTransformer = FieldToTransformer(client)
    return fields.map { it.visit(fieldToTransformer) }
}

interface ResponseTransformer {
    fun process(response: Response): Any?
}

class BodyResponseTransformer(client: AconiteClient, desc: BodyFieldDesc) : ResponseTransformer {
    private val deserializer = client.bodySerializer.create(desc.property, desc.property.returnType) ?:
            throw AconiteException("No suitable serializer found for body property '${desc.property}'")
    private val optional = desc.isOptional

    override fun process(response: Response): Any? {
        val body = response.body
        return if (optional && body == null) {
            null
        } else {
            deserializer.deserialize(body!!)
        }
    }
}

class HeaderResponseTransformer(client: AconiteClient, desc: HeaderFieldDesc) : ResponseTransformer {
    private val deserializer = client.stringSerializer.create(desc.property, desc.property.returnType) ?:
            throw AconiteException("No suitable serializer found for header property '${desc.property}'")
    private val name = desc.name
    private val optional = desc.isOptional

    override fun process(response: Response): Any? {
        val header = response.headers[name]
        return if (optional && header == null) {
            null
        } else {
            deserializer.deserialize(header!!)
        }
    }
}