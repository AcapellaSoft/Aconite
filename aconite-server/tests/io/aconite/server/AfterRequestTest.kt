package io.aconite.server

import io.aconite.Request
import io.aconite.annotations.AfterRequest
import io.aconite.annotations.GET
import io.aconite.annotations.Header
import io.aconite.annotations.MODULE
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class AfterRequestTest {
    interface RootApi {
        @MODULE("/")
        suspend fun module(): ModuleApi
    }

    interface ModuleApi {
        @GET("/foo")
        suspend fun foo(): String
    }

    class RootApiImpl(private val ref: AtomicReference<String>) : RootApi {
        suspend override fun module() = DummyApiImpl(ref)
    }

    class DummyApiImpl(private val ref: AtomicReference<String>) : ModuleApi {
        @AfterRequest
        @Suppress("unused")
        suspend fun after() {
            ref.set("123")
        }

        suspend override fun foo(): String {
            return ref.get()
        }
    }

    class HeaderApiImpl(private val ref: AtomicReference<String>) : ModuleApi {
        @AfterRequest
        @Suppress("unused")
        suspend fun readHeader(@Header("Foo") foo: String) {
            ref.set(foo)
        }

        suspend override fun foo(): String {
            return ref.get()
        }
    }

    @Test
    fun testAfterInterceptorCalls() = asyncTest {
        val server = AconiteServer()
        val ref = AtomicReference("111")
        server.register<ModuleApi> { DummyApiImpl(ref) }

        val response = server.accept("/foo", Request(
                method = "GET"
        ))

        Assert.assertEquals("111", response.body())
        Assert.assertEquals("123", ref.get())
    }

    @Test
    fun testBeforeInterceptorReturnsHeader() = asyncTest {
        val server = AconiteServer()
        val ref = AtomicReference("111")
        server.register<ModuleApi> { HeaderApiImpl(ref) }

        val response = server.accept("/foo", Request(
                method = "GET",
                headers = mapOf("Foo" to "456")
        ))

        Assert.assertEquals("111", response.body())
        Assert.assertEquals("456", ref.get())
    }

    @Test
    fun testBeforeInterceptorCallsForInnerModule() = asyncTest {
        val server = AconiteServer()
        val ref = AtomicReference("111")
        server.register<RootApi> { RootApiImpl(ref) }

        server.accept("/foo", Request(
                method = "GET"
        ))

        Assert.assertEquals("123", ref.get())
    }
}