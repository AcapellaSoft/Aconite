package io.aconite.server

import io.aconite.Request
import io.aconite.Response

/**
 * Accepts request and transform it into response.
 * Can some how use inner acceptor to process request.
 */
interface RequestAcceptor {
    companion object {
        operator fun invoke(accept: suspend (url: String, request: Request) -> Response) = object: RequestAcceptor {
            override suspend fun accept(url: String, request: Request) = accept(url, request)
        }
    }

    interface Factory<out C> {
        fun create(inner: RequestAcceptor, configurator: C.() -> Unit): RequestAcceptor
    }

    suspend fun accept(url: String, request: Request): Response
}