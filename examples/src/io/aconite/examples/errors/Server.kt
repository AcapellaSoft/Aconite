package io.aconite.examples.errors

import io.aconite.HttpException
import io.aconite.server.AconiteServer
import io.aconite.server.errors.LogErrorHandler
import io.aconite.server.handlers.VertxHandler
import io.aconite.server.serverPipeline

class ErrorsHandler : ErrorsApi {
    override suspend fun runtimeError() {
        throw RuntimeException("something went wrong")
    }

    override suspend fun httpError() {
        throw HttpException(409, "conflict")
    }
}

fun main(args: Array<String>) {
    val server = serverPipeline {
        install(LogErrorHandler)
        install(AconiteServer) {
            register(ErrorsHandler(), ErrorsApi::class)
        }
    }
    VertxHandler.runServer(server, 8080)
}