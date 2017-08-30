package io.aconite.utils

import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.selects.SelectInstance
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

/**
 * Extension for calling asynchronous functions by reflection.
 * @receiver the called function
 * @param[args] arguments of the called function
 * @return result of the called function
 */
suspend fun <R> KFunction<R>.asyncCall(vararg args: Any?) = suspendCancellableCoroutine<R> { c ->
    try {
        val r = call(*args, c)
        if (r !== COROUTINE_SUSPENDED) c.resume(r)
    } catch (ex: InvocationTargetException) {
        throw ex.cause ?: ex
    }
}

fun <R> startCoroutine(continuation: Continuation<R>, block: suspend () -> R): Any? {
    block.startCoroutine(continuation)
    return COROUTINE_SUSPENDED
}

fun <T> channelOf(vararg items: T) = object : ReceiveChannel<T> {
    private var index = 0

    override val isClosedForReceive: Boolean
        get() = index >= items.size
    override val isEmpty: Boolean
        get() = index >= items.size

    override fun iterator() = object : ChannelIterator<T> {
        suspend override fun hasNext() = !isEmpty
        suspend override fun next() = receive()
    }

    override fun poll(): T? {
        if (isEmpty) return null
        val item = items[index]
        ++index
        return item
    }

    suspend override fun receive() = poll() ?: throw ClosedReceiveChannelException(null)
    suspend override fun receiveOrNull() = poll()

    override fun <R> registerSelectReceive(select: SelectInstance<R>, block: suspend (T) -> R) {
        TODO("not implemented")
    }

    override fun <R> registerSelectReceiveOrNull(select: SelectInstance<R>, block: suspend (T?) -> R) {
        TODO("not implemented")
    }
}

fun <T> T?.toChannel(): ReceiveChannel<T> {
    if (this == null) return emptyChannel()
    return channelOf(this)
}

fun <T> emptyChannel(): ReceiveChannel<T> = Channel<T>().apply { close() }

suspend fun <T> buildChannel(
        context: CoroutineContext, capacity: Int = 0, builder: suspend ProducerScope<T>.() -> Unit
): ReceiveChannel<T> {
    return produce(context, capacity) {
        builder()
        close()
    }.channel
}