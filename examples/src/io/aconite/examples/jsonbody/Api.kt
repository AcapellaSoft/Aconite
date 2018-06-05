package io.aconite.examples.jsonbody

import io.aconite.annotations.Body
import io.aconite.annotations.POST

data class Data(
        val foo: Int,
        val bar: Int
)

interface JsonBodyApi {
    @POST("/")
    suspend fun withBody(@Body data: Data): Int
}