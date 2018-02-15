package io.aconite.parser

import kotlin.reflect.KParameter

interface ArgumentDesc {
    val parameter: KParameter
    val isOptional: Boolean

    fun <R> visit(visitor: Visitor<R>): R

    interface Visitor<out R> {
        fun header(desc: HeaderArgumentDesc): R
        fun path(desc: PathArgumentDesc): R
        fun query(desc: QueryArgumentDesc): R
        fun body(desc: BodyArgumentDesc): R
    }
}