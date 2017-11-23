package io.aconite.client

import io.aconite.BodyBuffer
import io.aconite.Buffer
import io.aconite.Request
import io.aconite.Response
import io.aconite.annotations.*
import kotlinx.coroutines.experimental.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

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

fun body(s: String) = BodyBuffer(Buffer.wrap(s), "text/plain")

fun Response?.body() = this?.body?.content?.string!!

fun asyncTest(timeout: Long = 10, unit: TimeUnit = TimeUnit.SECONDS, block: suspend () -> Unit) {
    val f = CompletableFuture<Void>()
    var ex: Throwable? = null
    launch(Unconfined) {
        try {
            block()
        } catch (e: Throwable) {
            ex = e
        } finally {
            f.complete(null)
        }
    }
    try {
        f.get(timeout, unit)
    } catch (ex: TimeoutException) {
        throw CancellationException()
    }
    ex?.let { throw it }
}