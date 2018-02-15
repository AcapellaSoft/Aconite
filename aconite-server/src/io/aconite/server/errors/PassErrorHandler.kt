package io.aconite.server.errors

import io.aconite.*

object PassErrorHandler : RequestAcceptor.Factory<Unit> {
    override fun create(inner: RequestAcceptor, configurator: Unit.() -> Unit) = ErrorHandler(inner) { ex ->
        when (ex) {
            is HttpException -> ex.toResponse()
            else -> Response(
                    code = 500,
                    body = BodyBuffer(Buffer.wrap("Internal server error"), "text/plain")
            )
        }
    }
}