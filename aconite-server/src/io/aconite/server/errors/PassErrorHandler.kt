package io.aconite.server.errors

import io.aconite.*
import io.aconite.server.ServerRequestAcceptor

object PassErrorHandler : ServerRequestAcceptor.Factory<Unit> {
    override fun create(inner: ServerRequestAcceptor, configurator: Unit.() -> Unit) = ErrorHandler(inner) { ex ->
        when (ex) {
            is HttpException -> ex.toResponse()
            else -> Response(
                    code = 500,
                    body = BodyBuffer(Buffer.wrap("Internal server error"), "text/plain")
            )
        }
    }
}