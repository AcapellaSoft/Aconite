package io.aconite.server

import io.aconite.AconiteException
import io.aconite.ArgumentMissingException
import io.aconite.Request
import io.aconite.Response
import io.aconite.parser.*
import io.aconite.utils.asyncCall
import kotlin.reflect.KFunction

private class ArgumentToTransformer(private val server: AconiteServer) : ArgumentDesc.Visitor<RequestTransformer> {
    override fun header(desc: HeaderArgumentDesc) = HeaderRequestTransformer(server, desc)
    override fun path(desc: PathArgumentDesc) = PathRequestTransformer(server, desc)
    override fun query(desc: QueryArgumentDesc) = QueryRequestTransformer(server, desc)
    override fun body(desc: BodyArgumentDesc) = BodyRequestTransformer(server, desc)
}

internal fun transformRequestParams(server: AconiteServer, args: List<ArgumentDesc>): List<RequestTransformer> {
    val argToTransformer = ArgumentToTransformer(server)
    return args.map { it.visit(argToTransformer) }
}

internal interface RequestTransformer {
    val isOptional: Boolean
    val name: String
    fun check(request: Request): Boolean
    fun process(request: Request): Any?
}

private class BodyRequestTransformer(server: AconiteServer, desc: BodyArgumentDesc) : RequestTransformer {
    override val isOptional = desc.isOptional
    private val serializer = server.bodySerializer.create(desc.parameter, desc.parameter.type) ?:
            throw AconiteException("No suitable serializer found for body parameter '${desc.parameter}'")

    override val name = ""

    override fun check(request: Request) = isOptional || request.body != null

    override fun process(request: Request): Any? {
        return serializer.deserialize(request.body ?: return null)
    }
}

private class HeaderRequestTransformer(server: AconiteServer, desc: HeaderArgumentDesc) : RequestTransformer {
    override val isOptional = desc.isOptional
    private val serializer = server.stringSerializer.create(desc.parameter, desc.parameter.type) ?:
            throw AconiteException("No suitable serializer found for header parameter '${desc.parameter}'")

    override val name = desc.name

    override fun check(request: Request) = isOptional || request.headers.containsKey(name)

    override fun process(request: Request): Any? {
        val header = request.headers[name] ?: return null
        return serializer.deserialize(header)
    }
}

private class PathRequestTransformer(server: AconiteServer, desc: PathArgumentDesc) : RequestTransformer {
    override val isOptional = desc.isOptional
    private val serializer = server.stringSerializer.create(desc.parameter, desc.parameter.type) ?:
            throw AconiteException("No suitable serializer found for path parameter '${desc.parameter}'")

    override val name = desc.name

    override fun check(request: Request) = isOptional || request.path.containsKey(name)

    override fun process(request: Request): Any? {
        val header = request.path[name] ?: return null
        return serializer.deserialize(header)
    }
}

private class QueryRequestTransformer(server: AconiteServer, desc: QueryArgumentDesc) : RequestTransformer {
    override val isOptional = desc.isOptional
    private val serializer = server.stringSerializer.create(desc.parameter, desc.parameter.type) ?:
            throw AconiteException("No suitable serializer found for query parameter '${desc.parameter}'")

    override val name = desc.name

    override fun check(request: Request) = isOptional || request.query.containsKey(name)

    override fun process(request: Request): Any? {
        val header = request.query[name] ?: return null
        return serializer.deserialize(header)
    }
}

private class FieldToTransformer(private val server: AconiteServer) : FieldDesc.Visitor<ResponseTransformer> {
    override fun header(desc: HeaderFieldDesc) = HeaderResponseTransformer(server, desc)
    override fun body(desc: BodyFieldDesc) = BodyResponseTransformer(server, desc)
}

internal fun transformResponseParams(server: AconiteServer, desc: ComplexResponseDesc): List<ResponseTransformer> {
    val fieldTransformer = FieldToTransformer(server)
    return desc.fields.map { it.visit(fieldTransformer) }
}

internal interface ResponseTransformer {
    fun process(result: Any): Response
}

private class BodyResponseTransformer(server: AconiteServer, desc: BodyFieldDesc) : ResponseTransformer {
    private val getter = desc.property.getter
    private val serializer = server.bodySerializer.create(desc.property, desc.property.returnType) ?:
            throw AconiteException("No suitable serializer found for body property '${desc.property}'")

    override fun process(result: Any): Response {
        val value = getter.call(result)
        val data = serializer.serialize(value)
        return Response(body = data)
    }
}

private class HeaderResponseTransformer(server: AconiteServer, desc: HeaderFieldDesc) : ResponseTransformer {
    private val getter = desc.property.getter
    private val serializer = server.stringSerializer.create(desc.property, desc.property.returnType) ?:
            throw AconiteException("No suitable serializer found for header property '${desc.property}'")
    private val name = desc.name

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

    val values = args.map { it.process(request) }
    val result = asyncCall(obj, *values.toTypedArray())
    return result
}