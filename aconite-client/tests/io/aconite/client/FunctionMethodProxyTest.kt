package io.aconite.client

import io.aconite.Request
import io.aconite.Response
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.full.functions

class FunctionMethodProxyTest {
    @Test fun testPassProxy() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = body(r.method)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "GET")
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("GET", result)
    }

    @Test fun testBodyParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = r.body) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "GET")
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("qux", result)
    }

    @Test fun testHeaderParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = body(r.headers["opt"]!!)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "GET")
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("baz", result)
    }

    @Test fun testPathParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = body(r.path["key"]!!)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "GET")
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("foo", result)
    }

    @Test fun testQueryParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = body(r.query["version"]!!)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "GET")
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("bar", result)
    }

    @Test fun testAppendUrl() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { url, _ -> Response(body = body(url)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "GET")
        val result = proxy.call("/prefix", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("/prefix/test/url", result)
    }
}