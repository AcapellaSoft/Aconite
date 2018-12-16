package io.aconite.examples.complexresponse

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
    val api = client.create<ComplexResponseApi>()[address]

    runBlocking {
        println(api.complexResponse())
    }

    System.exit(0)
}