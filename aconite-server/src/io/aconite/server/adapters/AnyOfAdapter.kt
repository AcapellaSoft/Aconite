package io.aconite.server.adapters

import io.aconite.server.CallAdapter
import kotlin.reflect.KFunction

class AnyOfAdapter(vararg val adapters: CallAdapter): CallAdapter {
    override fun adapt(fn: KFunction<*>) = adapters
            .map { it.adapt(fn) }
            .firstOrNull { it != null }
}