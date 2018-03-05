package io.aconite.examples.jsonbody

import io.aconite.client.AconiteClient
import io.aconite.client.clientPipeline
import io.aconite.client.clients.VertxHttpClient
import io.aconite.serializers.MoshiBodySerializer
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    val client = AconiteClient(
            clientPipeline {
                install(VertxHttpClient)
            },
            bodySerializer = MoshiBodySerializer.Factory()
    )
    val address = "http://localhost:8080"
    val api = client.create<JsonBodyApi>()[address]

    runBlocking {
        println(api.withBody(Data(1, 2)))
    }

    System.exit(0)
}