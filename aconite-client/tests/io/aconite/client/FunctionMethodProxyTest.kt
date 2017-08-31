package io.aconite.client

import io.aconite.Request
import io.aconite.Response
import io.aconite.utils.toChannel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.full.functions

class FunctionMethodProxyTest {
    @Test fun testPassProxy() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = respBody(r.method)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "GET")
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("GET", result)
    }

    @Test fun testBodyParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = r.body.toChannel()) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "GET")
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("qux", result)
    }

    @Test fun testHeaderParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = respBody(r.headers["opt"]!!)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "GET")
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("baz", result)
    }

    @Test fun testPathParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = respBody(r.path["key"]!!)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "GET")
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("foo", result)
    }

    @Test fun testQueryParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = respBody(r.query["version"]!!)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "GET")
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("bar", result)
    }

    @Test fun testAppendUrl() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { url, _ -> Response(body = respBody(url)) }
        )
        val fn = TestModuleApi::class.functions.first { it.name == "get" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "GET")
        val result = proxy.call("/prefix", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("/prefix/test/url", result)
    }

    @Test fun testStreaming() = asyncTest {
        val client = AconiteClient(
                httpClient = StreamingTestHttpClient("foo", "bar", "baz")
        )
        val fn = TestModuleApi::class.functions.first { it.name == "streaming" }

        val proxy = FunctionMethodProxy(client, fn, "/test/url", "POST")

        @Suppress("UNCHECKED_CAST")
        val result = proxy.call("", Request(), emptyArray()) as ReceiveChannel<String>

        Assert.assertEquals("foo", result.receive())
        Assert.assertEquals("bar", result.receive())
        Assert.assertEquals("baz", result.receive())
        Assert.assertNull(result.receiveOrNull())
    }
}