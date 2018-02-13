package io.aconite.server

import io.aconite.ArgumentMissingException
import io.aconite.Request
import io.aconite.parser.HttpMethodDesc
import io.aconite.parser.ModuleParser
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CancellationException

private val server = AconiteServer(
        bodySerializer = TestBodySerializer.Factory(),
        stringSerializer = TestStringSerializer.Factory(),
        methodFilter = MethodFilterPassAll()
)

class MethodHandlerTest {
    @Test
    fun testAllParams() = asyncTest {
        val obj = TestModule()
        val fn = ModuleParser().parse(TestModuleApi::class)
                .methods.first { it.function.name == "get" }
        val handler = MethodHandler(server, fn as HttpMethodDesc)

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
        val fn = ModuleParser().parse(TestModuleApi::class)
                .methods.first { it.function.name == "get" }
        val handler = MethodHandler(server, fn as HttpMethodDesc)

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
        val fn = ModuleParser().parse(TestModuleApi::class)
                .methods.first { it.function.name == "get" }
        val handler = MethodHandler(server, fn as HttpMethodDesc)

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
        val fn = ModuleParser().parse(TestModuleApi::class)
                .methods.first { it.function.name == "get" }
        val handler = MethodHandler(server, fn as HttpMethodDesc)

        handler.accept(obj, "/", Request(
                method = "GET",
                query = mapOf("version" to "123")
        ))
        Assert.assertTrue(false)
    }

    @Test
    fun testNotDefaultName() = asyncTest {
        val obj = TestModule()
        val fn = ModuleParser().parse(TestModuleApi::class)
                .methods.first { it.function.name == "post" }
        val handler = MethodHandler(server, fn as HttpMethodDesc)

        val response = handler.accept(obj, "/", Request(
                method = "POST",
                path = mapOf("key-in-path" to "abc")
        ))

        Assert.assertEquals("abc", response.body())
    }

    @Test(expected = CancellationException::class)
    fun testMethodCallCancellation() = asyncTest(1) {
        val obj = RootModule()
        val fn = ModuleParser().parse(RootModuleApi::class)
                .methods.first { it.function.name == "putInfinite" }
        val handler = MethodHandler(server, fn as HttpMethodDesc)
        handler.accept(obj, "/", Request("PUT"))
    }
}