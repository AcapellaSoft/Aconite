package io.aconite.server.adapters

import io.aconite.server.CallAdapter
import kotlin.reflect.KFunction

object SuspendCallAdapter : CallAdapter {
    override fun adapt(fn: KFunction<*>) = if (fn.isSuspend) fn else null
}