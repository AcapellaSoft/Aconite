package io.aconite.parser

import io.aconite.utils.UrlTemplate
import kotlin.reflect.KFunction

interface MethodDesc {
    val function: KFunction<*>
    val url: UrlTemplate
    val type: String

    fun <R> visit(visitor: Visitor<R>): R

    interface Visitor<out R> {
        fun module(desc: ModuleMethodDesc): R
        fun http(desc: HttpMethodDesc): R
        fun webSocket(desc: WebSocketMethodDesc): R
    }
}