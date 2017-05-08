package io.aconite.utils

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

fun resolve(parent: KType, child: KType): KType {
    val cls = child.classifier
    return when (cls) {
        null -> child
        is KClass<*> -> resolveArgs(parent, child)
        is KTypeParameter -> resolveArgs(parent, resolveParam(parent, cls) ?: child)
        else -> throw UnsupportedOperationException("Not supported type class ${child.javaClass}")
    }
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
