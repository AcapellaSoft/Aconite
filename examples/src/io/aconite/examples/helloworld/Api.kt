package io.aconite.examples.helloworld

import io.aconite.annotations.GET
import io.aconite.annotations.Query

interface HelloWorldApi {
    @GET("/")
    suspend fun helloWorld(@Query name: String): String
}