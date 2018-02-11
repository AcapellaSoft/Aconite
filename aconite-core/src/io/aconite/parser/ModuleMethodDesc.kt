package io.aconite.parser

import io.aconite.utils.UrlTemplate
import kotlin.reflect.KFunction

data class ModuleMethodDesc(
        override val url: UrlTemplate,
        override val function: KFunction<*>,
        val arguments: List<ArgumentDesc>,
        val response: ModuleDesc
) : MethodDesc {
    override fun <R> visit(visitor: MethodDesc.Visitor<R>) = visitor.module(this)
    override val type = ""
}