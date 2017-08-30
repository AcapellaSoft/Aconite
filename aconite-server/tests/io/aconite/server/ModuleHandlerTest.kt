package io.aconite.server

import io.aconite.Request
import kotlinx.coroutines.experimental.CancellationException
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.full.createType
import kotlin.reflect.full.functions

private val server = AconiteServer(
        bodySerializer = TestBodySerializer.Factory(),
        stringSerializer = TestStringSerializer.Factory(),
        callAdapter = TestCallAdapter,
        methodFilter = MethodFilterPassSpecified("get", "post")
)

class ModuleHandlerTest {

    @Test
    fun testGet() = asyncTest {
        val test = RootModuleApi::class.functions.first { it.name == "test" }
        val module = ModuleHandler(server, TestModuleApi::class.createType(), test)
        val root = RootModule()
        val response = module.accept(root, "/kv/keys/abc", Request(
                method = "GET",
                query = mapOf("version" to "123"),
                headers = mapOf("opt" to "baz"),
                body = body("body_str")
        ))
        Assert.assertEquals("key = abc, version = 123, opt = baz, body = body_str", response.body())
    }

    @Test
    fun testPost() = asyncTest {
        val test = RootModuleApi::class.functions.first { it.name == "test" }
        val module = ModuleHandler(server, TestModuleApi::class.createType(), test)
        val root = RootModule()
        val response = module.accept(root, "/kv/keys2/foobar", Request(
                method = "POST"
        ))
        Assert.assertEquals("foobar", response.body())
    }

    @Test(expected = CancellationException::class)
    fun testMethodCallCancellation() = asyncTest(1) {
        val obj = RootModule()
        val fn = RootModuleApi::class.functions.first { it.name == "testInfinite" }
        val handler = ModuleHandler(server, TestModuleApi::class.createType(), fn)
        handler.accept(obj, "/foo/bar/inf", Request("PUT"))
    }
}