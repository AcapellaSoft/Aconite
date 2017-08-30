package io.aconite.server

import io.aconite.*
import io.aconite.annotations.*
import io.aconite.server.adapters.SuspendCallAdapter
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import kotlinx.coroutines.experimental.withTimeout
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KFunction
import kotlin.reflect.KType

@Suppress("unused")
interface RootModuleApi {
    @MODULE("/foo/bar") suspend fun test(): TestModuleApi
    @MODULE("/foo/bar/inf") suspend fun testInfinite(): TestModuleApi
    @PATCH suspend fun patch(@Body newValue: String): String
    @PUT suspend fun putInfinite(): String
}

interface TestModuleApi {
    @GET("/kv/keys/{key}")
    suspend fun get(@Path key: String, @Query version: String, @Header opt: String? = null, @Body body: String? = null): String

    @PUT("/kv/keys/{key}")
    suspend fun putNotAnnotated(key: String): String

    @POST("/kv/keys2/{key-in-path}")
    suspend fun post(@Path("key-in-path") key: String): String
}

interface TestModuleMixedCallsApi {
    @GET suspend fun get(@Body value: String): String
    @POST fun post(@Body value: String): CompletableFuture<String>
}

@Suppress("unused")
class RootModule: RootModuleApi {
    override suspend fun test() = TestModule()
    override suspend fun testInfinite() = suspendCancellableCoroutine<TestModuleApi> { /* will block forever */ }
    override suspend fun patch(newValue: String) = "newValue = $newValue"
    override suspend fun putInfinite() = suspendCancellableCoroutine<String> { /* will block forever */ }
}

@Suppress("unused")
open class TestModule: TestModuleApi {
    override suspend fun get(key: String, version: String, opt: String?, body: String?)
            = "key = $key, version = $version, opt = ${opt ?: "foobar"}, body = $body"
    override suspend fun putNotAnnotated(key: String) = key
    override suspend fun post(key: String) = key
}

@Suppress("unused")
open class TestModuleMixedCalls: TestModuleMixedCallsApi {
    override suspend fun get(value: String) = value
    override fun post(value: String) = CompletableFuture.completedFuture(value)!!
}

class TestBodySerializer: BodySerializer {
    class Factory: BodySerializer.Factory {
        override fun create(annotations: KAnnotatedElement, type: KType)
                = if (type.classifier == String::class) TestBodySerializer() else null
    }

    override fun serialize(obj: Any?): Buffer {
        return Buffer.wrap(obj as String)
    }

    override fun deserialize(body: Buffer): Any? {
        return body.string
    }
}

class TestStringSerializer: StringSerializer {

    class Factory: StringSerializer.Factory {
        override fun create(annotations: KAnnotatedElement, type: KType)
                = if (type.classifier == String::class) TestStringSerializer() else null
    }

    override fun serialize(obj: Any?): String {
        return obj as String
    }

    override fun deserialize(s: String): Any? {
        return s
    }
}

object TestCallAdapter: CallAdapter {
    override fun adapt(fn: KFunction<*>) = fn
}

class MethodFilterPassAll: MethodFilter {
    override fun predicate(fn: KFunction<*>) = true
}

class MethodFilterPassSpecified(vararg val methods: String): MethodFilter {
    override fun predicate(fn: KFunction<*>) = fn.name in methods
}

object EmptyAnnotations: KAnnotatedElement {
    override val annotations: List<Annotation> get() = emptyList()
}

suspend fun Response?.body() = this?.body?.receive()?.string!!

fun body(s: String) = Buffer.wrap(s)

fun asyncTest(timeout: Long = 10, unit: TimeUnit = TimeUnit.SECONDS, block: suspend () -> Unit) = runBlocking {
    withTimeout(timeout, unit, block)
}
