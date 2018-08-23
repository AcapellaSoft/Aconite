package io.aconite.server.errors

import io.aconite.Request
import io.aconite.Response
import io.aconite.server.RequestInfo
import io.aconite.server.ServerRequestAcceptor

abstract class ErrorHandler(private val inner: ServerRequestAcceptor) : ServerRequestAcceptor {
    companion object {
        operator fun invoke(inner: ServerRequestAcceptor, handler: (Exception) -> Response) = object : ErrorHandler(inner) {
            override fun handle(ex: Exception) = handler(ex)
        }
    }

    final override suspend fun accept(info: RequestInfo, request: Request): Response {
        return try {
            inner.accept(info, request)
        } catch (ex: Exception) {
            handle(ex)
        }
    }

    abstract fun handle(ex: Exception): Response
}