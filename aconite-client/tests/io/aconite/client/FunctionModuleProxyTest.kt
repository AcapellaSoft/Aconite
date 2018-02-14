package io.aconite.client

import io.aconite.Request
import io.aconite.parser.ModuleMethodDesc
import io.aconite.parser.ModuleParser
import org.junit.Assert
import org.junit.Test

class FunctionModuleProxyTest {
    @Test fun testCreatesModuleProxy() = asyncTest {
        val client = AconiteClient(httpClient = TestHttpClient())
        val desc = ModuleParser().parse(RootModuleApi::class).methods
                .first { it.resolvedFunction.name == "test" }

        val proxy = FunctionModuleProxy(client, desc as ModuleMethodDesc)
        val result = proxy.call("", Request("GET", body = body("foobar")), emptyArray())

        Assert.assertTrue(result is TestModuleApi)
    }
}