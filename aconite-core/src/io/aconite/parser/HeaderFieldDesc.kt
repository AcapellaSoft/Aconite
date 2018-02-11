package io.aconite.parser

import kotlin.reflect.KProperty

data class HeaderFieldDesc(
        override val property: KProperty<*>,
        val name: String
) : FieldDesc {
    override fun <R> visit(visitor: FieldDesc.Visitor<R>) = visitor.header(this)
}