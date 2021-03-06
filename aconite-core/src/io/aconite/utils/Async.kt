package io.aconite.utils

import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.startCoroutine
import kotlin.reflect.KFunction

/**
 * Extension for calling asynchronous functions by reflection.
 * @receiver the called function
 * @param[args] arguments of the called function
 * @return result of the called function
 */
suspend fun <R> KFunction<R>.asyncCall(vararg args: Any?): R {
    return suspendCancellableCoroutine { cont ->
        try {
            val r = call(*args, cont)
            if (r !== COROUTINE_SUSPENDED) cont.resume(r)
        } catch (ex: InvocationTargetException) {
            throw ex.cause ?: ex
        }
    }
}

fun <R> startCoroutine(continuation: Continuation<R>, block: suspend () -> R): Any? {
    block.startCoroutine(continuation)
    return COROUTINE_SUSPENDED
}