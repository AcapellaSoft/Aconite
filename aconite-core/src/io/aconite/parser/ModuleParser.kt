package io.aconite.parser

import io.aconite.AconiteException
import io.aconite.annotations.*
import io.aconite.utils.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.*

class ModuleParser {
    companion object {
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
    }

    object MethodComparator : Comparator<MethodDesc> {
        override fun compare(a: MethodDesc, b: MethodDesc): Int {
            a.url.compareTo(b.url).let { if (it != 0) return -it }
            a.type.compareTo(b.type).let { if (it != 0) return it }
            return 0
        }
    }

    fun parse(iface: KClass<*>) = parse(iface.createType())

    private fun parse(iface: KType): ModuleDesc {
        val clazz = iface.cls()
        val methods = mutableListOf<MethodDesc>()

        for (fn in clazz.functions) {
            val (url, method) = fn.getHttpMethod() ?: continue
            val resolved = resolve(iface, fn)
            val handler = when (method) {
                null -> parseModuleMethod(url, resolved)
                else -> parseHttpMethod(url, method, resolved)
            }
            methods.add(handler)
        }

        val sortedMethods = methods.sortedWith(MethodComparator)
        return ModuleDesc(clazz, sortedMethods)
    }

    private fun parseModuleMethod(url: String, fn: KFunction<*>): ModuleMethodDesc {
        val arguments = parseArguments(fn)
        val responseType = fn.asyncReturnType()
        return ModuleMethodDesc(UrlTemplate(url), fn, arguments, parse(responseType))
    }

    private fun parseHttpMethod(url: String, method: String, fn: KFunction<*>): HttpMethodDesc {
        val arguments = parseArguments(fn)
        val response = parseResponse(fn)
        return HttpMethodDesc(UrlTemplate(url), fn, method, arguments, response)
    }

    fun parseArguments(fn: KFunction<*>) = fn.parameters.mapNotNull { parseArgument(it) }

    private fun parseArgument(param: KParameter): ArgumentDesc? {
        if (param.kind == KParameter.Kind.INSTANCE)
            return null
        if (param.kind == KParameter.Kind.EXTENSION_RECEIVER)
            throw AconiteException("Extension methods are not allowed")

        val annotations = param.annotations.filter { it.annotationClass in REQUEST_PARAM_ANNOTATIONS }
        if (annotations.isEmpty()) throw AconiteException("Parameter '$param' is not annotated")
        if (annotations.size > 1) throw AconiteException("Parameter '$param' has more than one annotations")
        val annotation = annotations.first()

        val isOptional = param.type.isMarkedNullable

        return when (annotation) {
            is Body -> BodyArgumentDesc(param, isOptional)
            is Header -> HeaderArgumentDesc(param, isOptional, name(param, annotation.name))
            is Path -> PathArgumentDesc(param, isOptional, name(param, annotation.name))
            is Query -> QueryArgumentDesc(param, isOptional, name(param, annotation.name))
            else -> throw RuntimeException("Unknown annotation $annotation") // should not happen
        }
    }

    private fun parseResponse(fn: KFunction<*>): ResponseDesc {
        val type = fn.asyncReturnType()
        val clazz = type.cls()

        return if (clazz.findAnnotation<ResponseClass>() != null) {
            if (type.isMarkedNullable)
                throw AconiteException("Return type of method '$fn' must not be nullable")
            val constructor = clazz.primaryConstructor
                    ?: throw AconiteException("Response class '$clazz' must have primary constructor")
            val resolved = resolve(type, constructor)
            val fields = resolved.parameters.map { parseField(type, it) }
            ComplexResponseDesc(type, constructor, fields)
        } else {
            BodyResponseDesc(type)
        }
    }

    private fun parseField(type: KType, param: KParameter): FieldDesc {
        val annotations = param.annotations.filter { it.annotationClass in RESPONSE_PARAM_ANNOTATIONS }
        if (annotations.isEmpty()) throw AconiteException("Parameter '$param' is not annotated")
        if (annotations.size > 1) throw AconiteException("Parameter '$param' has more than one annotations")
        val annotation = annotations.first()

        val clazz = type.cls()
        val property = clazz.memberProperties
                .find { it.name == param.name }
                ?: throw AconiteException("All parameters of response class '$clazz' must be a properties")
        val resolvedProperty = resolve(type, property)

        return when (annotation) {
            is Body -> BodyFieldDesc(resolvedProperty)
            is Header -> HeaderFieldDesc(resolvedProperty, name(param, annotation.name))
            else -> throw RuntimeException("Unknown annotation $annotation") // should not happen
        }
    }

    private fun name(param: KParameter, annotationName: String) =
            if (annotationName.isNotEmpty()) annotationName else param.name!!
}