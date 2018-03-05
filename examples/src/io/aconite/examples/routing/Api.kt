package io.aconite.examples.routing

import io.aconite.annotations.GET
import io.aconite.annotations.POST
import io.aconite.annotations.PUT
import io.aconite.annotations.Path

// All rules sorted in order from more specific to less specific.
// Static paths have higher priority then parameterized paths.
// So request `/foo` will be processed by getStaticFoo, and not by getWithPathArg.

interface RoutingApi {
    @GET("/")
    suspend fun getRoot(): String

    @POST("/")
    suspend fun postRoot(): String

    @PUT("/")
    suspend fun putRoot(): String

    @GET("/foo")
    suspend fun getStaticFoo(): String

    @GET("/bar")
    suspend fun getStaticBar(): String

    @GET("/{arg}")
    suspend fun getWithPathArg(@Path arg: String): String
}