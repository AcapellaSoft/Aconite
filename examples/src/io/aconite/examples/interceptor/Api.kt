package io.aconite.examples.interceptor

import io.aconite.annotations.Body
import io.aconite.annotations.PUT
import io.aconite.annotations.Path

interface SomeApi {
    @PUT("/foo/{bar}")
    suspend fun put(@Path bar: String, @Body data: String): String
}