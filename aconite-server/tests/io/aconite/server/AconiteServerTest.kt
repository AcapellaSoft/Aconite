package io.aconite.server

import io.aconite.Request
import io.aconite.Response
import io.aconite.server.errors.PassErrorHandler
import org.junit.Assert
import org.junit.Test

class AconiteServerTest {

    @Test
    fun testRegisterAccepted() = asyncTest {
        val server = AconiteServer(
                bodySerializer = TestBodySerializer.Factory(),
                stringSerializer = TestStringSerializer.Factory(),
                methodFilter = MethodFilterPassSpecified("get", "post", "test", "patch")
        )
        server.register(RootModule(), RootModuleApi::class)
        val response = server.accept(RequestInfo("/foo/bar/kv/keys/abc"), Request(
                method = "GET",
                query = mapOf("version" to "123")
        ))
        Assert.assertEquals("key = abc, version = 123, opt = foobar, body = null", response.body())
    }

    @Test
    fun testRegisterNotAccepted() = asyncTest {
        val server = serverPipeline {
            install(PassErrorHandler)
            install(AconiteServer) {
                bodySerializer = TestBodySerializer.Factory()
                stringSerializer = TestStringSerializer.Factory()
                methodFilter = MethodFilterPassSpecified("get", "post", "test", "patch")

                register(RootModule(), RootModuleApi::class)
            }
        }
        val response = server.accept(RequestInfo("/foo/bar/kv/keys/abc"), Request(
                method = "DELETE",
                query = mapOf("version" to "123")
        ))
        Assert.assertEquals(405, response.code)
    }

    @Test
    fun testRegisterFactory() = asyncTest {
        val server = AconiteServer(
                bodySerializer = TestBodySerializer.Factory(),
                stringSerializer = TestStringSerializer.Factory(),
                methodFilter = MethodFilterPassSpecified("get", "post", "test", "patch")
        )

        var counter = 0

        server.register<RootModuleApi> {
            ++counter
            RootModule()
        }

        for (i in 1..2) {
            server.accept(RequestInfo("/foo/bar/kv/keys/abc"), Request(
                    method = "GET",
                    query = mapOf("version" to "123")
            ))
        }

        Assert.assertEquals(2, counter)
    }

    private class TestRequestAcceptor(private val action: () -> Unit) : ServerRequestAcceptor.Factory<Unit> {
        override fun create(inner: ServerRequestAcceptor, configurator: Unit.() -> Unit) = object : ServerRequestAcceptor {
            override suspend fun accept(info: RequestInfo, request: Request): Response {
                action()
                return inner.accept(info, request)
            }
        }
    }

    @Test
    fun testServerPipelineOrder() = asyncTest {
        val order = mutableListOf<String>()

        val server = serverPipeline {
            install(TestRequestAcceptor { order.add("first") })
            install(TestRequestAcceptor { order.add("second") })
            install(TestRequestAcceptor { order.add("third") })
        }
        server.accept(RequestInfo("/foo"), Request())

        Assert.assertEquals(listOf("first", "second", "third"), order)
    }
}