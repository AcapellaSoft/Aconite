package io.aconite.client.adapters

import io.aconite.client.CallAdapter
import io.aconite.utils.startCoroutine
import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.KFunction

class SuspendCallAdapter(override val function: KFunction<*>) : CallAdapter {
    object Factory: CallAdapter.Factory {
        override fun create(fn: KFunction<*>) = if (fn.isSuspend) SuspendCallAdapter(fn) else null
    }

    @Suppress("UNCHECKED_CAST")
    override fun call(args: Array<Any?>, fn: suspend (Array<Any?>) -> Any?): Any? {
        val continuation = args.last() as Continuation<Any?>
        val realArgs = args.sliceArray(0..args.size - 1)
        return startCoroutine(continuation) { fn(realArgs) }
    }
}