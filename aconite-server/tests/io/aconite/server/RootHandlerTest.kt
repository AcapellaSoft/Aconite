package io.aconite.server

import org.junit.Assert
import org.junit.Test
import kotlin.reflect.full.createType

private val server = AconiteServer(
        bodySerializer = TestBodySerializer.Factory(),
        stringSerializer = TestStringSerializer.Factory(),
        callAdapter = TestCallAdapter(),
        methodFilter = MethodFilterPassSpecified("get", "post", "test", "patch")
)

class RootHandlerTest {

    @Test
    fun testInnerModuleGet() = asyncTest {
        val root = RootHandler(server, RootModuleApi::class.createType())
        val obj = RootModule()
        val response = root.accept(obj, "/foo/bar/kv/keys/abc", Request(
                method = "GET",
                query = mapOf("version" to "123")
        ))
        Assert.assertEquals("key = abc, version = 123, opt = foobar, body = null", response.body())
    }

    @Test
    fun testInnerModulePost() = asyncTest {
        val root = RootHandler(server, RootModuleApi::class.createType())
        val obj = RootModule()
        val response = root.accept(obj, "/foo/bar/kv/keys/abc", Request(
                method = "POST"
        ))
        Assert.assertEquals("abc", response.body())
    }

    @Test
    fun testRootModulePatch() = asyncTest {
        val root = RootHandler(server, RootModuleApi::class.createType())
        val obj = RootModule()
        val response = root.accept(obj, "/foo/bar", Request(
                method = "PATCH",
                body = body("12345")
        ))
        Assert.assertEquals("newValue = 12345", response.body())
    }
}