package io.aconite.server

import io.aconite.Request
import io.aconite.Response

/**
 * Accepts request and transform it into response.
 * Can some how use inner acceptor to process request.
 */
interface RequestAcceptor {
    interface Factory<out C> {
        fun create(inner: RequestAcceptor, configurator: C.() -> Unit): RequestAcceptor
    }

    abstract class DelegatedFactory<out C>(
            private val fn: (RequestAcceptor, C.() -> Unit) -> RequestAcceptor
    ) : Factory<C> {
        override fun create(inner: RequestAcceptor, configurator: C.() -> Unit): RequestAcceptor {
            return fn(inner, configurator)
        }
    }

    suspend fun accept(url: String, request: Request): Response
}