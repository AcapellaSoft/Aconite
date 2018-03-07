package io.aconite.examples.interceptor

import io.aconite.Request
import io.aconite.RequestAcceptor
import io.aconite.Response
import org.slf4j.LoggerFactory

class Interceptor(private val inner: RequestAcceptor, private val prefix: String) : RequestAcceptor {
    companion object : RequestAcceptor.Factory<Config> {
        private val logger = LoggerFactory.getLogger(Interceptor::class.java)

        override fun create(inner: RequestAcceptor, configurator: Config.() -> Unit): RequestAcceptor {
            val config = Config().apply(configurator)
            return Interceptor(inner, config.prefix)
        }
    }

    class Config {
        var prefix = ""
    }

    override suspend fun accept(url: String, request: Request): Response {
        val response = inner.accept(url, request)
        logger.info("$prefix: url = $url, request = $request, response = $response")
        return response
    }
}