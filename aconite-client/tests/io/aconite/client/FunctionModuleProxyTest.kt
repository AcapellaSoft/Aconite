package io.aconite.client

import io.aconite.Request
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.full.functions

class FunctionModuleProxyTest {
    @Test fun testCreatesModuleProxy() = asyncTest {
        val client = AconiteClient(httpClient = TestHttpClient())
        val fn = RootModuleApi::class.functions.first { it.name == "test" }

        val proxy = FunctionModuleProxy(client, fn, "/test/url")
        val result = proxy.call("", Request("GET", body = reqBody("foobar")), emptyArray())

        Assert.assertTrue(result is TestModuleApi)
    }
}