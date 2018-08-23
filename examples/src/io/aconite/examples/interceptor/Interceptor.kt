package io.aconite.examples.interceptor

import io.aconite.Request
import io.aconite.Response
import io.aconite.client.ClientRequestAcceptor
import io.aconite.server.RequestInfo
import io.aconite.server.ServerRequestAcceptor
import org.slf4j.LoggerFactory

class ClientInterceptor(private val inner: ClientRequestAcceptor, private val prefix: String) : ClientRequestAcceptor {
    companion object : ClientRequestAcceptor.Factory<Config> {
        private val logger = LoggerFactory.getLogger(ClientInterceptor::class.java)

        override fun create(inner: ClientRequestAcceptor, configurator: Config.() -> Unit): ClientRequestAcceptor {
            val config = Config().apply(configurator)
            return ClientInterceptor(inner, config.prefix)
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

class ServerInterceptor(private val inner: ServerRequestAcceptor, private val prefix: String) : ServerRequestAcceptor {
    companion object : ServerRequestAcceptor.Factory<Config> {
        private val logger = LoggerFactory.getLogger(ClientInterceptor::class.java)

        override fun create(inner: ServerRequestAcceptor, configurator: Config.() -> Unit): ServerRequestAcceptor {
            val config = Config().apply(configurator)
            return ServerInterceptor(inner, config.prefix)
        }
    }

    class Config {
        var prefix = ""
    }

    override suspend fun accept(info: RequestInfo, request: Request): Response {
        val response = inner.accept(info, request)
        logger.info("$prefix: info = $info, request = $request, response = $response")
        return response
    }
}