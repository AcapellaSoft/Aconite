package io.aconite.server

import io.aconite.Request
import io.aconite.annotations.GET
import io.aconite.annotations.Header
import io.aconite.annotations.MODULE
import org.junit.Assert
import org.junit.Test

interface ContextTestRoot {
    @MODULE("/foo")
    suspend fun foo(@Header first: String?): ContextTestModule
}

interface ContextTestModule {
    @GET("/bar")
    suspend fun bar(@Header second: String?): String

    @GET("/baz")
    suspend fun baz(@Header code: Int): String
}

class ContextTestRootImpl : ContextTestRoot {
    suspend override fun foo(first: String?): ContextTestModule {
        if (first != null)
            response.putHeader("header", first)
        return ContextTestModuleImpl()
    }
}

class ContextTestModuleImpl : ContextTestModule {
    suspend override fun bar(second: String?): String {
        if (second != null)
            response.putHeader("header", second)
        return ""
    }

    suspend override fun baz(code: Int): String {
        response.setStatusCode(code)
        return ""
    }
}

class ResponseContextTest {
    @Test
    fun testRootHeaderPassing() = asyncTest {
        val aconite = AconiteServer()
        aconite.register(ContextTestRootImpl(), ContextTestRoot::class)

        val response = aconite.accept("/foo/bar", Request(
                method = "GET",
                headers = mapOf(
                        "first" to "123"
                )
        ))

        Assert.assertEquals("123", response?.headers?.get("header"))
    }

    @Test
    fun testModuleHeaderPassing() = asyncTest {
        val aconite = AconiteServer()
        aconite.register(ContextTestRootImpl(), ContextTestRoot::class)

        val response = aconite.accept("/foo/bar", Request(
                method = "GET",
                headers = mapOf(
                        "second" to "456"
                )
        ))

        Assert.assertEquals("456", response?.headers?.get("header"))
    }

    @Test
    fun testModuleHeaderOverridesRootHeader() = asyncTest {
        val aconite = AconiteServer()
        aconite.register(ContextTestRootImpl(), ContextTestRoot::class)

        val response = aconite.accept("/foo/bar", Request(
                method = "GET",
                headers = mapOf(
                        "first" to "123",
                        "second" to "456"
                )
        ))

        Assert.assertEquals("456", response?.headers?.get("header"))
    }

    @Test
    fun testStatusCodePassing() = asyncTest {
        val aconite = AconiteServer()
        aconite.register(ContextTestRootImpl(), ContextTestRoot::class)

        val response = aconite.accept("/foo/baz", Request(
                method = "GET",
                headers = mapOf(
                        "code" to "123"
                )
        ))

        Assert.assertEquals(123, response?.code)
    }
}