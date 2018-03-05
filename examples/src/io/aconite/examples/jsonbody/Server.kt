package io.aconite.examples.jsonbody

import io.aconite.serializers.MoshiBodySerializer
import io.aconite.server.AconiteServer
import io.aconite.server.handlers.VertxHandler
import io.aconite.server.serverPipeline

class JsonBodyHandler : JsonBodyApi {
    override suspend fun withBody(data: Data): Int {
        return data.foo + data.bar
    }
}

fun main(args: Array<String>) {
    val server = serverPipeline {
        install(AconiteServer) {
            bodySerializer = MoshiBodySerializer.Factory()

            register(JsonBodyHandler(), JsonBodyApi::class)
        }
    }
    VertxHandler.runServer(server, 8080)
}