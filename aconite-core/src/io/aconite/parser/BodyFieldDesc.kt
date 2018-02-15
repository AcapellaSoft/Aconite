package io.aconite.parser

import kotlin.reflect.KProperty

data class BodyFieldDesc(
        override val isOptional: Boolean,
        override val property: KProperty<*>
) : FieldDesc {
    override fun <R> visit(visitor: FieldDesc.Visitor<R>) = visitor.body(this)
}