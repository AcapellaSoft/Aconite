package io.aconite.server.errors

import io.aconite.*
import io.aconite.server.*
import io.aconite.utils.toChannel
import org.slf4j.LoggerFactory

class LogErrorHandler(cls: Class<*> = LogErrorHandler::class.java): ErrorHandler {
    private val logger = LoggerFactory.getLogger(cls)

    override fun handle(ex: Throwable) = when (ex) {
        is HttpException -> ex.toResponse()
        else -> {
            logger.error("Internal server error", ex)
            Response(
                    code = 500,
                    body = Buffer.wrap("Internal server error").toChannel()
            )
        }
    }
}