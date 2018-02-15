package io.aconite.parser

import io.aconite.utils.UrlTemplate
import kotlin.reflect.KFunction

data class HttpMethodDesc(
        override val url: UrlTemplate,
        override val resolvedFunction: KFunction<*>,
        override val originalFunction: KFunction<*>,
        val method: String,
        val arguments: List<ArgumentDesc>,
        val response: ResponseDesc
) : MethodDesc {
    override fun <R> visit(visitor: MethodDesc.Visitor<R>) = visitor.http(this)
    override val type = method
}