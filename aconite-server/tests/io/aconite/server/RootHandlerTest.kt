package io.aconite.server

import io.aconite.Request
import io.aconite.parser.ModuleParser
import org.junit.Assert
import org.junit.Test

private val server = AconiteServer(
        bodySerializer = TestBodySerializer.Factory(),
        stringSerializer = TestStringSerializer.Factory(),
        methodFilter = MethodFilterPassSpecified("get", "post", "test", "patch")
)

class RootHandlerTest {
    @Test
    fun testInnerModuleGet() = asyncTest {
        val root = RootHandler(server, { RootModule() }, ModuleParser().parse(RootModuleApi::class))
        val response = root.accept("/foo/bar/kv/keys/abc", Request(
                method = "GET",
                query = mapOf("version" to "123")
        ))
        Assert.assertEquals("key = abc, version = 123, opt = foobar, body = null", response.body())
    }

    @Test
    fun testInnerModulePost() = asyncTest {
        val root = RootHandler(server, { RootModule() }, ModuleParser().parse(RootModuleApi::class))
        val response = root.accept("/foo/bar/kv/keys2/abc", Request(
                method = "POST"
        ))
        Assert.assertEquals("abc", response.body())
    }

    @Test
    fun testRootModulePatch() = asyncTest(1000) {
        val root = RootHandler(server, { RootModule() }, ModuleParser().parse(RootModuleApi::class))
        val response = root.accept("/", Request(
                method = "PATCH",
                body = body("12345")
        ))
        Assert.assertEquals("newValue = 12345", response.body())
    }
}