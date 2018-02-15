package io.aconite.parser

import kotlin.reflect.KParameter

data class HeaderArgumentDesc(
        override val parameter: KParameter,
        override val isOptional: Boolean,
        val name: String
) : ArgumentDesc {
    override fun <R> visit(visitor: ArgumentDesc.Visitor<R>) = visitor.header(this)
}