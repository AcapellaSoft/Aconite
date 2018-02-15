package io.aconite.server

import io.aconite.Request
import io.aconite.parser.ModuleMethodDesc
import io.aconite.parser.ModuleParser
import org.junit.Assert
import org.junit.Test

private val server = AconiteServer(
        bodySerializer = TestBodySerializer.Factory(),
        stringSerializer = TestStringSerializer.Factory(),
        methodFilter = MethodFilterPassSpecified("get", "post")
)

class ModuleHandlerTest {

    @Test
    fun testGet() = asyncTest {
        val test = ModuleParser().parse(RootModuleApi::class)
                .methods.first { it.resolvedFunction.name == "test" }
        val module = ModuleHandler(server, test as ModuleMethodDesc)
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
        val test = ModuleParser().parse(RootModuleApi::class)
                .methods.first { it.resolvedFunction.name == "test" }
        val module = ModuleHandler(server, test as ModuleMethodDesc)
        val root = RootModule()
        val response = module.accept(root, "/kv/keys2/foobar", Request(
                method = "POST"
        ))
        Assert.assertEquals("foobar", response.body())
    }
}