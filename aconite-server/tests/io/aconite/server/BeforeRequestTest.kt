package io.aconite.server

import io.aconite.Request
import io.aconite.annotations.BeforeRequest
import io.aconite.annotations.GET
import io.aconite.annotations.Header
import io.aconite.annotations.MODULE
import org.junit.Assert
import org.junit.Test

class BeforeRequestTest {
    interface RootApi {
        @MODULE("/")
        suspend fun module(): ModuleApi
    }

    interface ModuleApi {
        @GET("/foo")
        suspend fun foo(): String
    }

    class RootApiImpl : RootApi {
        suspend override fun module() = DummyApiImpl()
    }

    class DummyApiImpl : ModuleApi {
        private lateinit var foo: String

        @BeforeRequest
        @Suppress("unused")
        suspend fun before() {
            this.foo = "123"
        }

        suspend override fun foo(): String {
            return foo
        }
    }

    class HeaderApiImpl : ModuleApi {
        private lateinit var foo: String

        @BeforeRequest
        @Suppress("unused")
        suspend fun readHeader(@Header("Foo") foo: String) {
            this.foo = foo
        }

        suspend override fun foo(): String {
            return foo
        }
    }

    @Test
    fun testBeforeInterceptorCalls() = asyncTest {
        val server = AconiteServer()
        server.register<ModuleApi> { DummyApiImpl() }

        val response = server.accept(RequestInfo("/foo"), Request(
                method = "GET"
        ))

        Assert.assertEquals("123", response.body())
    }

    @Test
    fun testBeforeInterceptorReturnsHeader() = asyncTest {
        val server = AconiteServer()
        server.register<ModuleApi> { HeaderApiImpl() }

        val response = server.accept(RequestInfo("/foo"), Request(
                method = "GET",
                headers = mapOf("Foo" to "456")
        ))

        Assert.assertEquals("456", response.body())
    }

    @Test
    fun testBeforeInterceptorCallsForInnerModule() = asyncTest {
        val server = AconiteServer()
        server.register<RootApi> { RootApiImpl() }

        val response = server.accept(RequestInfo("/foo"), Request(
                method = "GET"
        ))

        Assert.assertEquals("123", response.body())
    }
}