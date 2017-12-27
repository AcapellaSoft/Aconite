package io.aconite.utils

import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine
import kotlin.reflect.KFunction

/**
 * This object can be used as the return value of the async function to indicate
 * that function was suspended.
 * TODO: find better way to use suspend with reflection
 */
val COROUTINE_SUSPENDED: Any = {
    val cls = Class.forName("kotlin.coroutines.experimental.intrinsics.IntrinsicsKt")
    val field = cls.getDeclaredField("COROUTINE_SUSPENDED")
    field.isAccessible = true
    field.get(null)
}()

private class CombinedContinuation<in T>(
        initContinuation: CancellableContinuation<T>,
        combineContext: CoroutineContext
) : CancellableContinuation<T> by initContinuation {
    override val context = initContinuation.context + combineContext
}

/**
 * Extension for calling asynchronous functions by reflection.
 * @receiver the called function
 * @param[context] additional context to merge with initial context
 * @param[args] arguments of the called function
 * @return result of the called function
 */
suspend fun <R> KFunction<R>.asyncCall(context: CoroutineContext, vararg args: Any?): R {
    return suspendCancellableCoroutine { cont ->
        try {
            val combined = CombinedContinuation(cont, context)
            val r = call(*args, combined)
            if (r !== COROUTINE_SUSPENDED) combined.resume(r)
        } catch (ex: InvocationTargetException) {
            throw ex.cause ?: ex
        }
    }
}

fun <R> startCoroutine(continuation: Continuation<R>, block: suspend () -> R): Any? {
    block.startCoroutine(continuation)
    return COROUTINE_SUSPENDED
}