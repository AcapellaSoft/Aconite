package io.aconite.server.errors

import io.aconite.*
import io.aconite.server.RequestAcceptor

class PassErrorHandler(inner: RequestAcceptor) : ErrorHandler(inner) {
    companion object : RequestAcceptor.DelegatedFactory<Unit>({ inner, _ -> PassErrorHandler(inner) })

    override fun handle(ex: Exception) = when (ex) {
        is HttpException -> ex.toResponse()
        else -> Response(
                code = 500,
                body = BodyBuffer(Buffer.wrap("Internal server error"), "text/plain")
        )
    }
}