package io.aconite.examples.args

import io.aconite.annotations.*

interface ArgsApi {
    @POST("/{foo}/bar/{baz}")
    suspend fun withArgs(
            @Path foo: String,
            @Path baz: Int,
            @Query qux: Double,
            @Header("Some-Header") someHeader: Boolean,
            @Body data: String
    ): String
}