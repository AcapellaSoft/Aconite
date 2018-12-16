package io.aconite.examples.interceptor

import io.aconite.client.AconiteClient
import io.aconite.client.clientPipeline
import io.aconite.client.clients.VertxHttpClient
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val client = AconiteClient(clientPipeline {
        install(ClientInterceptor) {
            prefix = "request logger"
        }
        install(VertxHttpClient)
    })
    val address = "http://localhost:8080"
    val api = client.create<SomeApi>()[address]

    runBlocking {
        println(api.put("123", "456"))
    }

    System.exit(0)
}