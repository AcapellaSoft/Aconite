package io.aconite.server

import io.aconite.annotations.*
import kotlinx.coroutines.experimental.future.future
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KFunction

private val server = AconiteServer(
        bodySerializer = TestBodySerializer.Factory(),
        stringSerializer = TestStringSerializer.Factory(),
        callAdapter = TestCallAdapter(),
        methodFilter = TestMethodFilter()
)

@Suppress("unused")
class TestModule {
    @GET("/kv/keys/{key}")
    fun get(
            @Path key: String,
            @Query version: String,
            @Header opt: String = "foobar",
            @Body body: String? = null
    ): CompletableFuture<String> = future {
        "key = $key, version = $version, opt = $opt, body = $body"
    }

    @PUT("/kv/keys/{key}")
    fun putNotAnnotated(key: String) = CompletableFuture.completedFuture(key)!!

    @POST("/kv/keys")
    fun post(@Path("key-in-path") key: String) = CompletableFuture.completedFuture(key)!!
}

class MethodHandlerTest {

    @Test
    fun testAllParams() = asyncTest {
        val obj = TestModule()
        val cls = TestModule::class
        val fn = cls.members.first { it.name == "get" } as KFunction<*>
        val handler = MethodHandler(server, "GET", fn)

        val response = handler.accept(obj, "", Request(
                method = "GET",
                path = mapOf("key" to "abc"),
                query = mapOf("version" to "123"),
                headers = mapOf("opt" to "baz"),
                body = body("body_str")
        ))

        Assert.assertEquals("key = abc, version = 123, opt = baz, body = body_str", response.body())
    }

    @Test
    fun testAllParamsWrongMethod() = asyncTest {
        val obj = TestModule()
        val cls = TestModule::class
        val fn = cls.members.first { it.name == "get" } as KFunction<*>
        val handler = MethodHandler(server, "GET", fn)

        val response = handler.accept(obj, "", Request(
                method = "POST",
                path = mapOf("key" to "abc"),
                query = mapOf("version" to "123"),
                headers = mapOf("opt" to "baz"),
                body = body("body_str")
        ))
        Assert.assertNull(response)
    }

    @Test
    fun testDefaultValues() = asyncTest {
        val obj = TestModule()
        val cls = TestModule::class
        val fn = cls.members.first { it.name == "get" } as KFunction<*>
        val handler = MethodHandler(server, "GET", fn)

        val response = handler.accept(obj, "", Request(
                method = "GET",
                path = mapOf("key" to "abc"),
                query = mapOf("version" to "123")
        ))

        Assert.assertEquals("key = abc, version = 123, opt = foobar, body = null", response.body())
    }

    @Test
    fun testNotAccepted() = asyncTest {
        val obj = TestModule()
        val cls = TestModule::class
        val fn = cls.members.first { it.name == "get" } as KFunction<*>
        val handler = MethodHandler(server, "GET", fn)

        val response = handler.accept(obj, "", Request(
                method = "GET",
                query = mapOf("version" to "123")
        ))

        Assert.assertNull(response)
    }

    @Test()
    fun testNotAnnotated() = asyncTest {
        val cls = TestModule::class
        val fn = cls.members.first { it.name == "putNotAnnotated" } as KFunction<*>
        try {
            MethodHandler(server, "GET", fn)
            Assert.assertTrue(false)
        } catch (ex: AconiteServerException) {
            Assert.assertTrue(true)
        }
    }

    @Test
    fun testNotDefaultName() = asyncTest {
        val obj = TestModule()
        val cls = TestModule::class
        val fn = cls.members.first { it.name == "post" } as KFunction<*>
        val handler = MethodHandler(server, "POST", fn)

        val response = handler.accept(obj, "", Request(
                method = "POST",
                path = mapOf("key-in-path" to "abc")
        ))

        Assert.assertEquals("abc", response.body())
    }
}