package io.aconite.parser

import kotlin.reflect.KType

data class BodyResponseDesc(
        val type: KType
) : ResponseDesc {
    override fun <R> visit(visitor: ResponseDesc.Visitor<R>) = visitor.body(this)
}