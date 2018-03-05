package io.aconite.examples.routing

import io.aconite.server.AconiteServer
import io.aconite.server.handlers.VertxHandler
import io.aconite.server.serverPipeline

class RoutingHandler : RoutingApi {
    override suspend fun getRoot(): String {
        return "getRoot"
    }

    override suspend fun postRoot(): String {
        return "postRoot"
    }

    override suspend fun putRoot(): String {
        return "putRoot"
    }

    override suspend fun getStaticFoo(): String {
        return "getStaticFoo"
    }

    override suspend fun getStaticBar(): String {
        return "getStaticBar"
    }

    override suspend fun getWithPathArg(arg: String): String {
        return "getWithPathArg($arg)"
    }
}

fun main(args: Array<String>) {
    val server = serverPipeline {
        install(AconiteServer) {
            register(RoutingHandler(), RoutingApi::class)
        }
    }
    VertxHandler.runServer(server, 8080)
}