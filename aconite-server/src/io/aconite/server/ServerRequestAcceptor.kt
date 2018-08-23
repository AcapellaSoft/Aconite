package io.aconite.server

import io.aconite.Request
import io.aconite.Response

enum class HttpVersion {
    HTTP_1_0,
    HTTP_1_1,
    HTTP_2_0,
}

/**
 * Extended request information for server-side.
 */
data class RequestInfo(
        val url: String,
        val remoteClient: String = "",
        val protocolVersion: HttpVersion = HttpVersion.HTTP_1_0
)

/**
 * Accepts request and transform it into response.
 * Can some how use inner acceptor to process request.
 */
interface ServerRequestAcceptor {
    companion object {
        operator fun invoke(accept: suspend (info: RequestInfo, request: Request) -> Response) = object: ServerRequestAcceptor {
            override suspend fun accept(info: RequestInfo, request: Request) = accept(info, request)
        }
    }

    interface Factory<out C> {
        fun create(inner: ServerRequestAcceptor, configurator: C.() -> Unit): ServerRequestAcceptor
    }

    suspend fun accept(info: RequestInfo, request: Request): Response
}