package io.aconite.examples.interceptor

import io.aconite.server.AconiteServer
import io.aconite.server.handlers.VertxHandler
import io.aconite.server.serverPipeline

class SomeHandler : SomeApi {
    override suspend fun put(bar: String, data: String): String {
        return "$bar $data"
    }
}

fun main(args: Array<String>) {
    val server = serverPipeline {
        install(ServerInterceptor) {
            prefix = "request logger"
        }
        install(AconiteServer) {
            register(SomeHandler(), SomeApi::class)
        }
    }
    VertxHandler.runServer(server, 8080)
}