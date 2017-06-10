package io.aconite.utils

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.KFunction

val COROUTINE_SUSPENDED = Any()

class MyContinuation<in R>(val c: Continuation<R>): Continuation<R> {
    override val context: CoroutineContext
        get() = c.context

    override fun resume(value: R) {
        if (value === COROUTINE_SUSPENDED) return
        c.resume(value)
    }

    override fun resumeWithException(exception: Throwable) {
        if (exception === COROUTINE_SUSPENDED) return
        c.resumeWithException(exception)
    }
}

suspend fun <R> KFunction<R>.asyncCall(vararg args: Any?) = suspendCoroutine<R> { c ->
    val cc = MyContinuation(c)
    val r = call(*args, cc)
    cc.resume(r)
}