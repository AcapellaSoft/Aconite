package io.aconite.server

import org.junit.Assert
import org.junit.Test
import kotlin.reflect.full.createType

private val server = AconiteServer(
        bodySerializer = TestBodySerializer.Factory(),
        stringSerializer = TestStringSerializer.Factory(),
        callAdapter = TestCallAdapter(),
        methodFilter = MethodFilterPassSpecified("get", "post")
)

class ModuleHandlerTest {

    @Test
    fun testGet() = asyncTest {
        val module = ModuleHandler(server, TestModuleApi::class.createType(), RootModuleApi::test)
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
        val module = ModuleHandler(server, TestModuleApi::class.createType(), RootModuleApi::test)
        val root = RootModule()
        val response = module.accept(root, "/kv/keys/foobar", Request(
                method = "POST"
        ))
        Assert.assertEquals("foobar", response.body())
    }
}