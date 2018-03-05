package io.aconite.examples.optional

import io.aconite.annotations.GET
import io.aconite.annotations.Query

// If argument marked nullable and it is not presented in the request,
// then null value will be passed to the function call.
// If required (not-null) argument is not presented,
// then client will receive "missing argument" response.

interface HelloWorldApi {
    @GET("/")
    suspend fun helloWorld(@Query name: String? = null): String
}