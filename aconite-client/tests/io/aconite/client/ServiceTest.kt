package io.aconite.client

import io.aconite.Response
import io.aconite.annotations.GET
import io.aconite.annotations.Header
import io.aconite.annotations.ResponseClass
import io.aconite.serializers.Cookie
import org.junit.Assert
import org.junit.Test

class ServiceTest {
    @ResponseClass
    data class CookieResponse(
            @Header("Cookie") val cookie: Cookie? = null
    )

    interface CookieTestApi {
        @GET("/foo")
        suspend fun foo(): CookieResponse
    }

    @Test
    fun testSetCookie() = asyncTest {
        val client = AconiteClient(
                acceptor = TestHttpClient { _, r -> Response(headers = r.headers) }
        )
        val service = client.create<CookieTestApi>()
        service.setCookie(Cookie(mapOf("123" to "456")))

        val api = service["http://localhost"]
        val response = api.foo()

        Assert.assertEquals("456", response.cookie?.data?.get("123"))
    }

    @Test
    fun testClearCookie() = asyncTest {
        val client = AconiteClient(
                acceptor = TestHttpClient { _, r -> Response(headers = r.headers) }
        )
        val service = client.create<CookieTestApi>()
        service.setCookie(Cookie(mapOf("123" to "456")))
        service.clearCookie()

        val api = service["http://localhost"]
        val response = api.foo()

        Assert.assertNull(response.cookie?.data?.get("123"))
    }
}