package io.aconite.server

import io.aconite.annotations.*
import kotlinx.coroutines.experimental.future.future
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KFunction
import kotlin.reflect.KType

@Suppress("unused")
interface RootModuleApi {
    @MODULE("/foo/bar")
    fun test(): CompletableFuture<TestModuleApi>

    @PATCH
    fun patch(@Body newValue: String): CompletableFuture<String>
}

interface TestModuleApi {
    @GET("/kv/keys/{key}")
    fun get(@Path key: String, @Query version: String, @Header opt: String? = null, @Body body: String? = null): CompletableFuture<String>

    @PUT("/kv/keys/{key}")
    fun putNotAnnotated(key: String): CompletableFuture<String>

    @POST("/kv/keys2/{key-in-path}")
    fun post(@Path("key-in-path") key: String): CompletableFuture<String>
}

@Suppress("unused")
class RootModule: RootModuleApi {

    override fun test() = future<TestModuleApi> {
        TestModule()
    }

    override fun patch(newValue: String) = future {
        "newValue = $newValue"
    }
}

@Suppress("unused")
open class TestModule: TestModuleApi {

    final override fun get(key: String, version: String, opt: String?, body: String?) = future {
        "key = $key, version = $version, opt = ${opt ?: "foobar"}, body = $body"
    }

    override fun putNotAnnotated(key: String) = future {
        key
    }

    override fun post(key: String) = future {
        key
    }
}

class TestBodySerializer: BodySerializer {

    class Factory: BodySerializer.Factory {
        override fun create(annotations: KAnnotatedElement, type: KType)
                = if (type.classifier == String::class) TestBodySerializer() else null
    }

    override fun serialize(obj: Any?): BodyBuffer {
        return BodyBuffer(ByteBuffer.wrap((obj as String).toByteArray()), "plain/text")
    }

    override fun deserialize(body: BodyBuffer): Any? {
        return String((body.content as ByteBuffer).array())
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

class TestCallAdapter: CallAdapter {
    override fun adapt(fn: KFunction<*>) = fn
}

class MethodFilterPassAll: MethodFilter {
    override fun predicate(fn: KFunction<*>) = true
}

class MethodFilterPassSpecified(vararg val methods: String): MethodFilter {
    override fun predicate(fn: KFunction<*>) = fn.name in methods
}

fun Response?.body() = String((this?.body?.content as ByteBuffer).array())

fun body(s: String) = BodyBuffer(ByteBuffer.wrap(s.toByteArray()), "text/plain")

fun asyncTest(timeout: Long = 1, unit: TimeUnit = TimeUnit.SECONDS, block: suspend () -> Unit)
        = future { block() }.get(timeout, unit)!!
