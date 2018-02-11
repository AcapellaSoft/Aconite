package io.aconite.parser

import io.aconite.utils.UrlTemplate
import kotlin.reflect.KFunction

// todo: web socket support
data class WebSocketMethodDesc(
        override val url: UrlTemplate,
        override val function: KFunction<*>,
        val arguments: List<ArgumentDesc>,
        val response: ResponseDesc
) : MethodDesc {
    override fun <R> visit(visitor: MethodDesc.Visitor<R>) = visitor.webSocket(this)
    override val type = "WS"
}