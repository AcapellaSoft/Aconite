package io.aconite.server.errors

import io.aconite.*
import io.aconite.server.*
import io.aconite.utils.toChannel

object PassErrorHandler: ErrorHandler {
    override fun handle(ex: Throwable) = when (ex) {
        is HttpException -> ex.toResponse()
        else -> Response(
                code = 500,
                body = Buffer.wrap("Internal server error").toChannel()
        )
    }
}