package io.aconite.parser

import kotlin.reflect.KProperty

interface FieldDesc {
    val property: KProperty<*>

    fun <R> visit(visitor: Visitor<R>): R

    interface Visitor<out R> {
        fun header(desc: HeaderFieldDesc): R
        fun body(desc: BodyFieldDesc): R
    }
}