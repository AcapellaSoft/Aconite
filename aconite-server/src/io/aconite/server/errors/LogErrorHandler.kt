package io.aconite.server.errors

import io.aconite.*
import io.aconite.server.RequestAcceptor
import org.slf4j.LoggerFactory

class LogErrorHandler(clazz: Class<*>, inner: RequestAcceptor): ErrorHandler(inner) {
    companion object : RequestAcceptor.DelegatedFactory<Configuration>({ inner, builder ->
        val config = Configuration().apply(builder)
        LogErrorHandler(config.clazz, inner)
    })

    class Configuration {
        var clazz: Class<*> = LogErrorHandler::class.java
    }

    private val logger = LoggerFactory.getLogger(clazz)

    override fun handle(ex: Exception) = when (ex) {
        is HttpException -> ex.toResponse()
        else -> {
            logger.error("Internal server error", ex)
            Response(
                    code = 500,
                    body = BodyBuffer(Buffer.wrap("Internal server error"), "text/plain")
            )
        }
    }
}