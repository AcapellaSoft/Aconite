package io.aconite.server.filters

import io.aconite.server.MethodFilter
import kotlin.reflect.KFunction

object PassMethodFilter: MethodFilter {
    override fun predicate(fn: KFunction<*>) = true
}