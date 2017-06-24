package io.aconite.server.errors

import io.aconite.HttpException
import io.aconite.server.*

object PassErrorHandler: ErrorHandler {
    override fun handle(ex: Throwable) = when (ex) {
        is HttpException -> ex.toResponse()
        else -> Response(
                code = 500,
                body = BodyBuffer(Buffer.wrap("Internal server error"), "text/plain")
        )
    }
}