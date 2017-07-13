package io.aconite.client

import io.aconite.Request
import io.aconite.Response
import org.junit.Assert
import org.junit.Test
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.full.createType
import kotlin.reflect.full.functions

class ModuleProxyTest {
    @Test fun testCreatesModuleProxy() {
        val client = AconiteClient(httpClient = TestHttpClient())
        val proxy = client.moduleFactory.create(RootModuleApi::class.createType())
        Assert.assertNotNull(proxy)
    }

    @Test fun testCallProxyMethod() = asyncTest {
        val client = AconiteClient(httpClient = TestHttpClient { _, r -> Response(body = r.body)})
        val proxy = client.moduleFactory.create(RootModuleApi::class.createType())
        val fn = RootModuleApi::class.functions.first { it.name == "patch" }

        val result = suspendCoroutine<Response> { c ->
            proxy.invoke(fn, "/test/url", Request(), arrayOf("foobar", c))
        }

        Assert.assertEquals("foobar", result.body())
    }

    @Test fun testCallProxyModule() = asyncTest {
        val client = AconiteClient(httpClient = TestHttpClient())
        val proxy = client.moduleFactory.create(RootModuleApi::class.createType())
        val fn = RootModuleApi::class.functions.first { it.name == "test" }

        val result = suspendCoroutine<Any> { c ->
            proxy.invoke(fn, "/test/url", Request(), arrayOf(c))
        }

        Assert.assertTrue(result is TestModuleApi)
    }
}