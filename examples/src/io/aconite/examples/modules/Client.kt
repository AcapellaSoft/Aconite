package io.aconite.examples.modules

import io.aconite.client.AconiteClient
import io.aconite.client.clientPipeline
import io.aconite.client.clients.VertxHttpClient
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    val client = AconiteClient(
            clientPipeline {
                install(VertxHttpClient)
            }
    )
    val address = "http://localhost:8080"
    val api = client.create<RootApi>()[address]

    runBlocking {
        println(api.foo().baz())
        println(api.bar().baz())
    }

    System.exit(0)
}