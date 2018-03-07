package io.aconite.examples.complexresponse

import io.aconite.server.AconiteServer
import io.aconite.server.errors.LogErrorHandler
import io.aconite.server.handlers.VertxHandler
import io.aconite.server.serverPipeline

class ComplexResponseHandler : ComplexResponseApi {
    override suspend fun complexResponse(): ResponseData {
        return ResponseData("foo", 123)
    }
}

fun main(args: Array<String>) {
    val server = serverPipeline {
        install(LogErrorHandler)
        install(AconiteServer) {
            register(ComplexResponseHandler(), ComplexResponseApi::class)
        }
    }
    VertxHandler.runServer(server, 8080)
}