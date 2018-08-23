package io.aconite.client.errors

import io.aconite.HttpException
import io.aconite.Request
import io.aconite.Response
import io.aconite.client.ClientRequestAcceptor

abstract class ErrorHandler(private val inner: ClientRequestAcceptor) : ClientRequestAcceptor {
    companion object {
        operator fun invoke(inner: ClientRequestAcceptor, handler: (Response) -> HttpException) = object : ErrorHandler(inner) {
            override fun handle(error: Response) = handler(error)
        }
    }

    final override suspend fun accept(url: String, request: Request): Response {
        val response = inner.accept(url, request)
        if (response.code ?: 200 != 200)
            throw handle(response)
        return response
    }

    abstract fun handle(error: Response): HttpException
}