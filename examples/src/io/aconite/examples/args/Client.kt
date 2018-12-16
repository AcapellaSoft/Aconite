package io.aconite.examples.args

import io.aconite.client.AconiteClient
import io.aconite.client.clientPipeline
import io.aconite.client.clients.VertxHttpClient
import io.aconite.client.errors.PassErrorHandler
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val client = AconiteClient(clientPipeline {
        install(PassErrorHandler)
        install(VertxHttpClient)
    })
    val address = "http://localhost:8080"
    val api = client.create<ArgsApi>()[address]

    runBlocking {
        println(api.withArgs("1", 2, 3.5, true, "4"))
    }

    System.exit(0)
}