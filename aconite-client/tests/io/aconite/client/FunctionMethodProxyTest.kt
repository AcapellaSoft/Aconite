package io.aconite.client

import io.aconite.Request
import io.aconite.Response
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.full.functions

class FunctionMethodProxyTest {
    @Test fun testPassProxy() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { Response(body = body(it.method)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn)
        val result = proxy.call(Request("GET"), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("GET", result.body())
    }

    @Test fun testBodyParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { Response(body = it.body) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn)
        val result = proxy.call(Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("qux", result.body())
    }

    @Test fun testHeaderParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { Response(body = body(it.headers["opt"]!!)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn)
        val result = proxy.call(Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("baz", result.body())
    }

    @Test fun testPathParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { Response(body = body(it.path["key"]!!)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn)
        val result = proxy.call(Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("foo", result.body())
    }

    @Test fun testQueryParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { Response(body = body(it.query["version"]!!)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn)
        val result = proxy.call(Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("bar", result.body())
    }
}