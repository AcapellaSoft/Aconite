package io.aconite.parser

import kotlin.reflect.KParameter

data class BodyArgumentDesc(
        override val parameter: KParameter,
        override val isOptional: Boolean
) : ArgumentDesc {
    override fun <R> visit(visitor: ArgumentDesc.Visitor<R>) = visitor.body(this)
}