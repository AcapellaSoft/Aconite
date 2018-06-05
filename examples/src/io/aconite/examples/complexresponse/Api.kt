package io.aconite.examples.complexresponse

import io.aconite.annotations.Body
import io.aconite.annotations.GET
import io.aconite.annotations.Header
import io.aconite.annotations.ResponseClass

@ResponseClass
data class ResponseData(
        @Body val body: String,
        @Header("Some-Header") val header: Int
)

interface ComplexResponseApi {
    @GET("/")
    suspend fun complexResponse(): ResponseData
}