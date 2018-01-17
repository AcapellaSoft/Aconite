package io.aconite.server

import io.aconite.Request
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
            @Header val header: String
    )

    @ResponseClass
    data class GenericResponse<out T>(
            @Header val header: T
    )

    interface Api {
        @GET("/foo")
        suspend fun foo(@Body body: String, @Header header: String): ComplexResponse

        @GET("/bar")
        suspend fun bar(@Header header: Int): GenericResponse<Int>
    }

    class ApiImpl : Api {
        suspend override fun foo(body: String, header: String): ComplexResponse {
            return ComplexResponse(body, header)
        }

        suspend override fun bar(header: Int): GenericResponse<Int> {
            return GenericResponse(header)
        }
    }

    @Test
    fun testComplexResponse() = asyncTest {
        val server = AconiteServer()
        server.register(ApiImpl(), Api::class)

        val response = server.accept("/foo", Request(
                method = "GET",
                body = body("123"),
                headers = mapOf("header" to "456")
        ))

        Assert.assertEquals("123", response.body())
        Assert.assertEquals("456", response?.headers?.get("header"))
    }

    @Test
    fun testGenericResponse() = asyncTest {
        val server = AconiteServer()
        server.register(ApiImpl(), Api::class)

        val response = server.accept("/bar", Request(
                method = "GET",
                headers = mapOf("header" to "456")
        ))

        Assert.assertEquals("456", response?.headers?.get("header"))
    }
}