package io.aconite.server

import io.aconite.annotations.*
import org.junit.Assert
import org.junit.Test

private val server = AconiteServer(
        bodySerializer = TestBodySerializer.Factory(),
        stringSerializer = TestStringSerializer.Factory(),
        callAdapter = TestCallAdapter(),
        methodFilter = TestMethodFilter()
)

@Suppress("unused")
class TestModule {
    @GET("/kv/keys/{key}")
    fun get(@Path key: String, @Query version: String, @Header opt: String = "foobar", @Body body: String? = null): String {
        return "key = $key, version = $version, opt = $opt, body = $body"
    }

    @PUT("/kv/keys/{key}")
    fun putNotAnnotated(key: String) = key

    @POST("/kv/keys")
    fun post(@Path("key-in-path") key: String) = key
}

class MethodHandlerTest {

    @Test
    fun testAllParams() {
        val obj = TestModule()
        val cls = TestModule::class
        val fn = cls.members.first { it.name == "get" }
        val handler = MethodHandler(server, fn)

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
    fun testDefaultValues() {
        val obj = TestModule()
        val cls = TestModule::class
        val fn = cls.members.first { it.name == "get" }
        val handler = MethodHandler(server, fn)

        val response = handler.accept(obj, "", Request(
                method = "GET",
                path = mapOf("key" to "abc"),
                query = mapOf("version" to "123")
        ))

        Assert.assertEquals("key = abc, version = 123, opt = foobar, body = null", response.body())
    }

    @Test
    fun testNotAccepted() {
        val obj = TestModule()
        val cls = TestModule::class
        val fn = cls.members.first { it.name == "get" }
        val handler = MethodHandler(server, fn)

        val response = handler.accept(obj, "", Request(
                method = "GET",
                query = mapOf("version" to "123")
        ))

        Assert.assertNull(response)
    }

    @Test(expected = AconiteServerException::class)
    fun testNotAnnotated() {
        val cls = TestModule::class
        val fn = cls.members.first { it.name == "putNotAnnotated" }
        MethodHandler(server, fn)
    }

    @Test
    fun testNotDefaultName() {
        val obj = TestModule()
        val cls = TestModule::class
        val fn = cls.members.first { it.name == "post" }
        val handler = MethodHandler(server, fn)

        val response = handler.accept(obj, "", Request(
                method = "POST",
                path = mapOf("key-in-path" to "abc")
        ))

        Assert.assertEquals("abc", response.body())
    }
}