package io.aconite.examples.helloworld

import io.aconite.server.AconiteServer
import io.aconite.server.handlers.VertxHandler
import io.aconite.server.serverPipeline

class HelloWorldHandler : HelloWorldApi {
    override suspend fun helloWorld(name: String): String {
        return "Hello, $name!"
    }
}

fun main(args: Array<String>) {
    val server = serverPipeline {
        install(AconiteServer) {
            register(HelloWorldHandler(), HelloWorldApi::class)
        }
    }
    VertxHandler.runServer(server, 8080)
}