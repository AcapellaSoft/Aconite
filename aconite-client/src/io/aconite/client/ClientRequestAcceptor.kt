package io.aconite.client

import io.aconite.Request
import io.aconite.Response

/**
 * Accepts request and transform it into response.
 * Can some how use inner acceptor to process request.
 */
interface ClientRequestAcceptor {
    companion object {
        operator fun invoke(accept: suspend (url: String, request: Request) -> Response) = object: ClientRequestAcceptor {
            override suspend fun accept(url: String, request: Request) = accept(url, request)
        }
    }

    interface Factory<out C> {
        fun create(inner: ClientRequestAcceptor, configurator: C.() -> Unit): ClientRequestAcceptor
    }

    suspend fun accept(url: String, request: Request): Response
}