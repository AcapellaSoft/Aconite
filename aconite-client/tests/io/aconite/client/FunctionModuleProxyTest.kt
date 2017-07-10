package io.aconite.client

import io.aconite.Request
import io.aconite.Response
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.full.functions

class FunctionModuleProxyTest {
    @Test fun testCreatesModuleProxy() = asyncTest {
        val client = AconiteClient(httpClient = TestHttpClient())
        val fn = RootModuleApi::class.functions.first { it.name == "test" }

        val proxy = FunctionModuleProxy(client, fn)
        val result = proxy.call(Request("GET", body = body("foobar")), emptyList())

        Assert.assertTrue(result is TestModuleApi)
    }

    @Test fun testPassMethodProxy() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { Response(body = body(it.method)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn)
        val result = proxy.call(Request("GET"), listOf("foo", "bar", null, null))

        Assert.assertEquals("GET", result.body())
    }
}