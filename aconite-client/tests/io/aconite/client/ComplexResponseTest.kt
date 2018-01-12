package io.aconite.client

import io.aconite.Response
import io.aconite.annotations.Body
import io.aconite.annotations.GET
import io.aconite.annotations.Header
import io.aconite.annotations.ResponseClass
import org.junit.Assert
import org.junit.Test

class ComplexResponseTest {
    @ResponseClass
    data class ComplexResponse(
            @Body val body: String,
            @Header val header: String?
    )

    @ResponseClass
    data class GenericResponse<out T>(
            @Header val header: T
    )

    interface Api {
        @GET("/foo")
        suspend fun foo(): ComplexResponse

        @GET("/bar")
        suspend fun bar(): GenericResponse<Int>
    }

    @Test
    fun testComplexResponse() = asyncTest {
        val client = AconiteClient(TestHttpClient { _, _ ->
            Response(
                    body = body("123"),
                    headers = mapOf("header" to "456")
            )
        })

        val api = client.create<Api>()["http://localhost"]
        val response = api.foo()

        Assert.assertEquals("123", response.body)
        Assert.assertEquals("456", response.header)
    }

    @Test
    fun testGenericResponse() = asyncTest {
        val client = AconiteClient(TestHttpClient { _, _ ->
            Response(
                    body = body("123"),
                    headers = mapOf("header" to "456")
            )
        })

        val api = client.create<Api>()["http://localhost"]
        val response = api.bar()

        Assert.assertEquals(456, response.header)
    }

    @Test
    fun testOptionalHeader() = asyncTest {
        val client = AconiteClient(TestHttpClient { _, _ ->
            Response(
                    body = body("123")
            )
        })

        val api = client.create<Api>()["http://localhost"]
        val response = api.foo()

        Assert.assertEquals("123", response.body)
        Assert.assertNull(response.header)
    }
}