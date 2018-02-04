package io.aconite.server.errors

import io.aconite.Request
import io.aconite.Response
import io.aconite.server.RequestAcceptor

abstract class ErrorHandler(private val inner: RequestAcceptor) : RequestAcceptor {
    companion object {
        operator fun invoke(inner: RequestAcceptor, handler: (Exception) -> Response) = object : ErrorHandler(inner) {
            override fun handle(ex: Exception) = handler(ex)
        }
    }

    final override suspend fun accept(url: String, request: Request): Response {
        return try {
            inner.accept(url, request)
        } catch (ex: Exception) {
            handle(ex)
        }
    }

    abstract fun handle(ex: Exception): Response
}