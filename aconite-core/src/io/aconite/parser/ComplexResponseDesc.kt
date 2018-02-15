package io.aconite.parser

import kotlin.reflect.KFunction
import kotlin.reflect.KType

data class ComplexResponseDesc(
        val type: KType,
        val constructor: KFunction<*>,
        val fields: List<FieldDesc>
) : ResponseDesc {
    override fun <R> visit(visitor: ResponseDesc.Visitor<R>) = visitor.complex(this)
}