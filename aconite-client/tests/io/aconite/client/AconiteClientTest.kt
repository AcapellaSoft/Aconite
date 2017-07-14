package io.aconite.client

import io.aconite.Response
import org.junit.Assert
import org.junit.Test

class AconiteClientTest {
    @Test fun testCreateRootProxy() {
        val client = AconiteClient(httpClient = TestHttpClient())
        val proxy = client.create<RootModuleApi>()
        Assert.assertNotNull(proxy)
    }

    @Test fun testCreateModuleProxy() {
        val client = AconiteClient(httpClient = TestHttpClient())
        val proxy = client.create<TestModuleApi>()
        Assert.assertNotNull(proxy)
    }

    @Test fun testCallMethodWithProxy() = asyncTest {
        val client = AconiteClient(httpClient = TestHttpClient { _, r -> Response(body = r.body) })
        val proxy = client.create<RootModuleApi>()

        val result = proxy.patch("foobar")
        Assert.assertEquals("foobar", result)
    }

    @Test fun testCallModuleWithProxy() = asyncTest {
        val client = AconiteClient(httpClient = TestHttpClient { _, r -> Response(body = r.body) })
        val proxy = client.create<RootModuleApi>()

        val result = proxy.test()
        Assert.assertNotNull(result)
    }

    @Test fun testCallMethodInModuleWithProxy() = asyncTest {
        val client = AconiteClient(httpClient = TestHttpClient { _, r -> Response(body = r.body) })
        val proxy = client.create<RootModuleApi>()

        val module = proxy.test()
        val result = module.get("key", "version", "opt", "body")
        Assert.assertEquals("body", result)
    }

    @Test fun testPathParameters() = asyncTest {
        val client = AconiteClient(httpClient = TestHttpClient { url, _ -> Response(body = body(url)) })
        val proxy = client.create<TestModuleApi>()

        val result = proxy.get("param", "version")
        Assert.assertEquals("/kv/keys/param", result)
    }
}