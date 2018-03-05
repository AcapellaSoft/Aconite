package io.aconite.examples.routing

import io.aconite.client.AconiteClient
import io.aconite.client.clientPipeline
import io.aconite.client.clients.VertxHttpClient
import kotlinx.coroutines.experimental.runBlocking

fun main(args: Array<String>) {
    val client = AconiteClient(clientPipeline {
        install(VertxHttpClient)
    })
    val address = "http://localhost:8080"
    val api = client.create<RoutingApi>()[address]

    runBlocking {
        println(api.getRoot())
        println(api.postRoot())
        println(api.putRoot())
        println(api.getStaticFoo())
        println(api.getStaticBar())
        println(api.getWithPathArg("123"))
    }

    System.exit(0)
}