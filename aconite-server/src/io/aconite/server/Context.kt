package io.aconite.server

import io.aconite.Response
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Contains reference to the [response] object. Only way to mutate the response is
 * to copy original response and set reference to new response.
 */
class CoroutineResponseReference(var response: Response) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<CoroutineResponseReference>
    override val key: CoroutineContext.Key<*> = Key
}

/**
 * Helper object for common operations with response context.
 * Response context is write only.
 */
object response {
    /**
     * Set header [key] of the response to [value].
     * Inner modules can overwrite value of this header.
     */
    suspend fun putHeader(key: String, value: String) = suspendCoroutine<Unit> { cont ->
        val response = cont.context[CoroutineResponseReference]!!
        response.response = response.response.copy(
                headers = response.response.headers + (key to value)
        )
        cont.resume(Unit)
    }

    /**
     * Set status code of the response to [value].
     * Inner modules can overwrite status code.
     */
    suspend fun setStatusCode(value: Int) = suspendCoroutine<Unit> { cont ->
        val response = cont.context[CoroutineResponseReference]!!
        response.response = response.response.copy(
                code = value
        )
        cont.resume(Unit)
    }
}