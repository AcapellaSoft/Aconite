package io.aconite.utils

import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.KFunction

/**
 * This object can be used as the return value of the async function to indicate
 * that function was suspended. This works only with function [asyncCall].
 */
val COROUTINE_SUSPENDED = Any()

private class MyContinuation<in R>(val c: Continuation<R>): Continuation<R> {
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

/**
 * Extension for calling asynchronous functions by reflection.
 * @receiver the called function
 * @param[args] arguments of the called function
 * @return result of the called function
 */
suspend fun <R> KFunction<R>.asyncCall(vararg args: Any?) = suspendCoroutine<R> { c ->
    val cc = MyContinuation(c)
    try {
        val r = call(*args, cc)
        cc.resume(r)
    } catch (ex: InvocationTargetException) {
        throw ex.cause ?: ex
    }
}