package io.aconite.server

import org.junit.Assert
import org.junit.Test

private val server = AconiteServer(
        bodySerializer = TestBodySerializer.Factory(),
        stringSerializer = TestStringSerializer.Factory(),
        callAdapter = TestCallAdapter(),
        methodFilter = MethodFilterPassAll()
)

class MethodHandlerTest {

    @Test
    fun testAllParams() = asyncTest {
        val obj = TestModule()
        val fn = TestModuleApi::get
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
        val fn = TestModuleApi::get
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
        val fn = TestModuleApi::get
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
        val fn = TestModuleApi::get
        val handler = MethodHandler(server, "GET", fn)

        val response = handler.accept(obj, "", Request(
                method = "GET",
                query = mapOf("version" to "123")
        ))

        Assert.assertNull(response)
    }

    @Test()
    fun testNotAnnotated() = asyncTest {
        val fn = TestModuleApi::putNotAnnotated
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
        val fn = TestModuleApi::post
        val handler = MethodHandler(server, "POST", fn)

        val response = handler.accept(obj, "", Request(
                method = "POST",
                path = mapOf("key-in-path" to "abc")
        ))

        Assert.assertEquals("abc", response.body())
    }
}