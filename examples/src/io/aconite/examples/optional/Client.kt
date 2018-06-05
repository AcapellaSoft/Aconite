package io.aconite.examples.optional

import io.aconite.client.AconiteClient
import io.aconite.client.clientPipeline
import io.aconite.client.clients.VertxHttpClient
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    val client = AconiteClient(clientPipeline {
                install(VertxHttpClient)
    })
    val address = "http://localhost:8080"
    val api = client.create<HelloWorldApi>()[address]

    runBlocking {
        println(api.helloWorld())
        println(api.helloWorld("Aconite"))
    }

    System.exit(0)
}