package io.aconite.server

import io.aconite.Request
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.full.createType

private val server = AconiteServer(
        bodySerializer = TestBodySerializer.Factory(),
        stringSerializer = TestStringSerializer.Factory(),
        callAdapter = TestCallAdapter,
        methodFilter = MethodFilterPassSpecified("get", "post", "test", "patch")
)

class RootHandlerTest {

    @Test
    fun testInnerModuleGet() = asyncTest {
        val root = RootHandler(server, RootModule(), RootModuleApi::class.createType())
        val response = root.accept("/foo/bar/kv/keys/abc", Request(
                method = "GET",
                query = mapOf("version" to "123")
        ))
        Assert.assertEquals("key = abc, version = 123, opt = foobar, body = null", response.body())
    }

    @Test
    fun testInnerModulePost() = asyncTest {
        val root = RootHandler(server, RootModule(), RootModuleApi::class.createType())
        val response = root.accept("/foo/bar/kv/keys2/abc", Request(
                method = "POST"
        ))
        Assert.assertEquals("abc", response.body())
    }

    @Test
    fun testRootModulePatch() = asyncTest(1000) {
        val root = RootHandler(server, RootModule(), RootModuleApi::class.createType())
        val response = root.accept("/", Request(
                method = "PATCH",
                body = body("12345")
        ))
        Assert.assertEquals("newValue = 12345", response.body())
    }
}