package io.aconite.utils

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.*
import kotlin.reflect.full.createType

/**
 * Function for resolving type parameters of [child] type into real types
 * using [parent] type. [parent] type can be a class containing a function
 * or property with a [child] return type. This function can by repeatedly
 * applied to the [child] type for resolving types from many sources.
 * @return [child] type with resolved type parameters
 */
fun resolve(parent: KType, child: KType): KType {
    val cls = child.classifier
    return when (cls) {
        null -> child
        is KClass<*> -> resolveArgs(parent, child)
        is KTypeParameter -> resolveArgs(parent, resolveParam(parent, cls) ?: child)
        else -> throw UnsupportedOperationException("Not supported type class ${child.javaClass}")
    }
}

/**
 * Function for resolving type parameters of arguments and return value of [fn].
 * This function can by repeatedly applied to the [fn] for resolving types
 * from many sources.
 * @return [fn] with resolved type parameters
 */
fun <R> resolve(parent: KType, fn: KFunction<R>) = object: KFunction<R> by fn {
    inner class KResolvedParameter(parameter: KParameter): KParameter by parameter {
        override val type by lazy { resolve(parent, parameter.type) }
    }

    override val returnType = resolve(parent, fn.returnType)
    override val parameters = fn.parameters.map(this::KResolvedParameter)
}

private fun resolveParam(parent: KType, param: KTypeParameter): KType? {
    val cls = (parent.classifier as KClass<*>)
    return cls.typeParameters.indices
            .filter { cls.typeParameters[it] == param }
            .map { parent.arguments[it].type }
            .firstOrNull()
}

private fun resolveArgs(parent: KType, child: KType) = child.classifier!!.createType(
        arguments = child.arguments.map { resolveProjection(parent, it) },
        nullable = child.isMarkedNullable
)

private fun resolveProjection(parent: KType, projection: KTypeProjection): KTypeProjection {
    val resolved = projection.type?.let { resolve(parent, it) }
    return KTypeProjection(projection.variance, resolved)
}

/**
 * Converts kotlin type to java type.
 * @receiver kotlin type
 * @param[wrap] need to wrap primitive types?
 * @return java type
 */
fun KType.toJavaType(wrap: Boolean = false): Type = when {
    arguments.isNotEmpty() -> object : ParameterizedType {
        private val rawType = (classifier as KClass<*>).java
        private val args = arguments.map { it.type?.toJavaType(true) }.toTypedArray()
        override fun getRawType() = rawType
        override fun getOwnerType() = null
        override fun getActualTypeArguments() = args
    }
    wrap -> Primitives.wrap((classifier as KClass<*>).java)
    else -> (classifier as KClass<*>).java
}