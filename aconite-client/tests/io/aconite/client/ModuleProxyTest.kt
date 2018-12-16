package io.aconite.client

import io.aconite.Request
import io.aconite.Response
import io.aconite.parser.ModuleParser
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.full.functions

class ModuleProxyTest {
    @Test fun testCreatesModuleProxy() {
        val client = AconiteClient(acceptor = TestHttpClient())
        val proxy = client.moduleFactory.create(ModuleParser().parse(RootModuleApi::class))
        Assert.assertNotNull(proxy)
    }

    @Test fun testCallProxyMethod() = asyncTest {
        val client = AconiteClient(acceptor = TestHttpClient { _, r -> Response(body = r.body)})
        val proxy = client.moduleFactory.create(ModuleParser().parse(RootModuleApi::class))
        val fn = RootModuleApi::class.functions.first { it.name == "patch" }

        val result = suspendCancellableCoroutine<Any?> { c ->
            proxy.invoke(fn, "/test/url", Request(), arrayOf("foobar", c))
        }

        Assert.assertEquals("foobar", result)
    }

    @Test fun testCallProxyModule() = asyncTest {
        val client = AconiteClient(acceptor = TestHttpClient())
        val proxy = client.moduleFactory.create(ModuleParser().parse(RootModuleApi::class))
        val fn = RootModuleApi::class.functions.first { it.name == "test" }

        val result = suspendCancellableCoroutine<Any> { c ->
            proxy.invoke(fn, "/test/url", Request(), arrayOf(c))
        }

        Assert.assertTrue(result is TestModuleApi)
    }
}