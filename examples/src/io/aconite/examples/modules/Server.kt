package io.aconite.examples.modules

import io.aconite.server.AconiteServer
import io.aconite.server.handlers.VertxHandler
import io.aconite.server.serverPipeline

class RootHandler : RootApi {
    override suspend fun foo() = ModuleHandler("foo")

    override suspend fun bar() = ModuleHandler("bar")
}

class ModuleHandler(private val base: String) : ModuleApi {
    override suspend fun baz(): String {
        return "$base/baz"
    }
}

fun main(args: Array<String>) {
    val server = serverPipeline {
        install(AconiteServer) {
            register(RootHandler(), RootApi::class)
        }
    }
    VertxHandler.runServer(server, 8080)
}