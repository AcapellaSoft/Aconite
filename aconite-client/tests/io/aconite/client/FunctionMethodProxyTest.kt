package io.aconite.client

import io.aconite.Request
import io.aconite.Response
import io.aconite.parser.HttpMethodDesc
import io.aconite.parser.ModuleParser
import org.junit.Assert
import org.junit.Test

class FunctionMethodProxyTest {
    @Test fun testPassProxy() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = body(r.method)) }
        )
        val desc = ModuleParser().parse(TestModuleApi::class).methods
                .first { it.resolvedFunction.name == "get" }

        val proxy = FunctionMethodProxy(client, desc as HttpMethodDesc)
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("GET", result)
    }

    @Test fun testBodyParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = r.body) }
        )
        val desc = ModuleParser().parse(TestModuleApi::class).methods
                .first { it.resolvedFunction.name == "get" }

        val proxy = FunctionMethodProxy(client, desc as HttpMethodDesc)
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("qux", result)
    }

    @Test fun testHeaderParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = body(r.headers["opt"]!!)) }
        )
        val desc = ModuleParser().parse(TestModuleApi::class).methods
                .first { it.resolvedFunction.name == "get" }

        val proxy = FunctionMethodProxy(client, desc as HttpMethodDesc)
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("baz", result)
    }

    @Test fun testPathParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = body(r.path["key"]!!)) }
        )
        val desc = ModuleParser().parse(TestModuleApi::class).methods
                .first { it.resolvedFunction.name == "get" }

        val proxy = FunctionMethodProxy(client, desc as HttpMethodDesc)
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("foo", result)
    }

    @Test fun testQueryParameter() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, r -> Response(body = body(r.query["version"]!!)) }
        )
        val desc = ModuleParser().parse(TestModuleApi::class).methods
                .first { it.resolvedFunction.name == "get" }

        val proxy = FunctionMethodProxy(client, desc as HttpMethodDesc)
        val result = proxy.call("", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("bar", result)
    }

    @Test fun testAppendUrl() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { url, _ -> Response(body = body(url)) }
        )
        val desc = ModuleParser().parse(TestModuleApi::class).methods
                .first { it.resolvedFunction.name == "get" }

        val proxy = FunctionMethodProxy(client, desc as HttpMethodDesc)
        val result = proxy.call("/prefix", Request(), arrayOf("foo", "bar", "baz", "qux"))

        Assert.assertEquals("/prefix/kv/keys/foo", result)
    }
}