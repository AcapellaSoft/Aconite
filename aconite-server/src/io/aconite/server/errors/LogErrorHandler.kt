package io.aconite.server.errors

import io.aconite.HttpException
import io.aconite.server.ErrorHandler
import org.slf4j.LoggerFactory

object LogErrorHandler: ErrorHandler {
    private val logger = LoggerFactory.getLogger(LogErrorHandler::class.java)

    override fun handle(ex: Throwable): HttpException? {
        logger.error("Internal server error", ex)
        return HttpException(500, "Internal server error", ex)
    }
}