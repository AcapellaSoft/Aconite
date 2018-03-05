package io.aconite.examples.args

import io.aconite.server.AconiteServer
import io.aconite.server.handlers.VertxHandler
import io.aconite.server.serverPipeline

class ArgsHandler : ArgsApi {
    override suspend fun withArgs(foo: String, baz: Int, qux: Double, someHeader: Boolean, data: String): String {
        return "foo=$foo, baz=$baz, qux=$qux, someHeader=$someHeader, data=$data"
    }
}

fun main(args: Array<String>) {
    val server = serverPipeline {
        install(AconiteServer) {
            register(ArgsHandler(), ArgsApi::class)
        }
    }
    VertxHandler.runServer(server, 8080)
}