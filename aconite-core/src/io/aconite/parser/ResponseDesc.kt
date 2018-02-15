package io.aconite.parser

interface ResponseDesc {
    fun <R> visit(visitor: Visitor<R>): R

    interface Visitor<out R> {
        fun body(desc: BodyResponseDesc): R
        fun complex(desc: ComplexResponseDesc): R
    }
}