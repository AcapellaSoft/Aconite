package io.aconite.parser

import kotlin.reflect.KParameter

data class QueryArgumentDesc(
        override val parameter: KParameter,
        override val isOptional: Boolean,
        val name: String
) : ArgumentDesc {
    override fun <R> visit(visitor: ArgumentDesc.Visitor<R>) = visitor.query(this)
}