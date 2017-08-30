package io.aconite.client

import io.aconite.Buffer
import io.aconite.Request
import io.aconite.Response
import io.aconite.annotations.*
import io.aconite.utils.toChannel
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import java.util.concurrent.TimeUnit

@Suppress("unused")
interface RootModuleApi {
    @MODULE("/foo/bar") suspend fun test(): TestModuleApi
    @MODULE("/foo/bar") suspend fun testWithParam(@Query param: String): TestModuleApi
    @PATCH suspend fun patch(@Body newValue: String): String
}

@Suppress("unused")
interface TestModuleApi {
    @GET("/kv/keys/{key}")
    suspend fun get(@Path key: String, @Query version: String, @Header opt: String? = null, @Body body: String? = null): String

    @POST("/kv/keys2/{key-in-path}")
    suspend fun post(@Path("key-in-path") key: String): String
}

class TestHttpClient(val handler: suspend (String, Request) -> Response): HttpClient {
    constructor(): this({ _, _ -> Response()})

    suspend override fun makeRequest(url: String, request: Request) = handler(url, request)
}

fun reqBody(s: String) = Buffer.wrap(s)

fun respBody(s: String) = Buffer.wrap(s).toChannel()

fun Response?.body() = this?.body?.poll()?.string!!

fun asyncTest(timeout: Long = 10, unit: TimeUnit = TimeUnit.SECONDS, block: suspend () -> Unit) = runBlocking {
    withTimeout(timeout, unit, block)
}