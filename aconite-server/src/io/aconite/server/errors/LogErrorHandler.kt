package io.aconite.server.errors

import io.aconite.*
import io.aconite.server.ServerRequestAcceptor
import org.slf4j.LoggerFactory

object LogErrorHandler : ServerRequestAcceptor.Factory<LogErrorHandler.Configuration> {
    class Configuration {
        var clazz: Class<*> = LogErrorHandler::class.java
    }

    override fun create(inner: ServerRequestAcceptor, configurator: Configuration.() -> Unit): ServerRequestAcceptor {
        val config = Configuration().apply(configurator)
        val logger = LoggerFactory.getLogger(config.clazz)

        return ErrorHandler(inner) { ex ->
            when (ex) {
                is HttpException -> ex.toResponse()
                else -> {
                    logger.error("Internal server error", ex)
                    Response (
                            code = 500,
                            body = BodyBuffer(Buffer.wrap("Internal server error"), "text/plain")
                    )
                }
            }
        }
    }
}