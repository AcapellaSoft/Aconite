package io.aconite.examples.modules

import io.aconite.annotations.GET
import io.aconite.annotations.MODULE

interface RootApi {
    @MODULE("/foo")
    suspend fun foo(): ModuleApi

    @MODULE("/bar")
    suspend fun bar(): ModuleApi
}

interface ModuleApi {
    @GET("/baz")
    suspend fun baz(): String
}