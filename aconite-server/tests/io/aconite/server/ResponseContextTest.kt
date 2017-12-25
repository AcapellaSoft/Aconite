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

    @GET("/cookie")
    suspend fun cookie()
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

    suspend override fun cookie() {
        response.setCookie(
                maxAge = 10,
                domain = "foo-bar.com",
                path = "/abc",
                sameSite = SameSiteType.STRICT,
                secure = true,
                httpOnly = true,
                cookies = mapOf(
                        "foo" to "bar",
                        "baz" to "123"
                )
        )
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

    @Test
    fun testSetCookie() = asyncTest {
        val aconite = AconiteServer()
        aconite.register(ContextTestRootImpl(), ContextTestRoot::class)

        val response = aconite.accept("/cookie", Request(
                method = "GET"
        ))

        val expected = "foo=bar; baz=123; MaxAge=10; Domain=foo-bar.com; Path=/abc; SameSite=STRICT; Secure; HttpOnly; "
        Assert.assertEquals(expected, response?.headers?.get("Set-Cookie"))
    }
}