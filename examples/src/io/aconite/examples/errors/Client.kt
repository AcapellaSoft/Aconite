package io.aconite.examples.errors

import io.aconite.HttpException
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
    val api = client.create<ErrorsApi>()[address]

    runBlocking {
        try {
            api.runtimeError()
        } catch (ex: HttpException) {
            ex.printStackTrace()
        }
        try {
            api.httpError()
        } catch (ex: HttpException) {
            ex.printStackTrace()
        }
    }

    System.exit(0)
}