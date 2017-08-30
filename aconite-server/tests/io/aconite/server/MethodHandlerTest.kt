package io.aconite.server

import io.aconite.AconiteException
import io.aconite.ArgumentMissingException
import io.aconite.Request
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CancellationException
import kotlin.reflect.full.functions

private val server = AconiteServer(
        bodySerializer = TestBodySerializer.Factory(),
        stringSerializer = TestStringSerializer.Factory(),
        callAdapter = TestCallAdapter,
        methodFilter = MethodFilterPassAll()
)

class MethodHandlerTest {

    @Test
    fun testAllParams() = asyncTest {
        val obj = TestModule()
        val fn = TestModuleApi::class.functions.first { it.name == "get" }
        val handler = MethodHandler(server, "GET", fn)

        val response = handler.accept(obj, "/", Request(
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
        val fn = TestModuleApi::class.functions.first { it.name == "get" }
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
        val fn = TestModuleApi::class.functions.first { it.name == "get" }
        val handler = MethodHandler(server, "GET", fn)

        val response = handler.accept(obj, "/", Request(
                method = "GET",
                path = mapOf("key" to "abc"),
                query = mapOf("version" to "123")
        ))

        Assert.assertEquals("key = abc, version = 123, opt = foobar, body = null", response.body())
    }

    @Test(expected = ArgumentMissingException::class)
    fun testNotAccepted() = asyncTest {
        val obj = TestModule()
        val fn = TestModuleApi::class.functions.first { it.name == "get" }
        val handler = MethodHandler(server, "GET", fn)

        handler.accept(obj, "/", Request(
                method = "GET",
                query = mapOf("version" to "123")
        ))
        Assert.assertTrue(false)
    }

    @Test(expected = AconiteException::class)
    fun testNotAnnotated() = asyncTest {
        val fn = TestModuleApi::class.functions.first { it.name == "putNotAnnotated" }
        MethodHandler(server, "GET", fn)
        Assert.assertTrue(false)
    }

    @Test
    fun testNotDefaultName() = asyncTest {
        val obj = TestModule()
        val fn = TestModuleApi::class.functions.first { it.name == "post" }
        val handler = MethodHandler(server, "POST", fn)

        val response = handler.accept(obj, "/", Request(
                method = "POST",
                path = mapOf("key-in-path" to "abc")
        ))

        Assert.assertEquals("abc", response.body())
    }

    @Test(expected = CancellationException::class)
    fun testMethodCallCancellation() = asyncTest(1) {
        val obj = RootModule()
        val fn = RootModuleApi::class.functions.first { it.name == "putInfinite" }
        val handler = MethodHandler(server, "PUT", fn)
        handler.accept(obj, "/", Request("PUT"))
    }

    @Test
    fun testMethodCallNotFound() = asyncTest(1) {
        val obj = RootModule()
        val fn = RootModuleApi::class.functions.first { it.name == "patch" }
        val handler = MethodHandler(server, "PATCH", fn)
        Assert.assertNull(handler.accept(obj, "/foobar", Request("PATCH")))
    }
}