package io.aconite.client

import io.aconite.BodyBuffer
import io.aconite.Buffer
import io.aconite.annotations.*
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.TimeUnit

@Suppress("unused")
interface RootModuleApi {
    @MODULE("/foo/bar") suspend fun test(): TestModuleApi
    @PATCH suspend fun patch(@Body newValue: String): String
}

@Suppress("unused")
interface TestModuleApi {
    @GET("/kv/keys/{key}")
    suspend fun get(@Path key: String, @Query version: String, @Header opt: String? = null, @Body body: String? = null): String

    @PUT("/kv/keys/{key}")
    suspend fun putNotAnnotated(key: String): String

    @POST("/kv/keys2/{key-in-path}")
    suspend fun post(@Path("key-in-path") key: String): String
}

fun body(s: String) = BodyBuffer(Buffer.wrap(s), "text/plain")

fun asyncTest(timeout: Long = 10, unit: TimeUnit = TimeUnit.SECONDS, block: suspend () -> Unit)
        = future { block() }.get(timeout, unit)!!