package io.aconite.client.errors

import io.aconite.HttpException
import io.aconite.Response
import io.aconite.client.*
import org.junit.Assert
import org.junit.Test

class PassErrorHandlerTest {
    @Test fun transformResponseToEx() {
        val response = Response(404, body = respBody("message"))
        val ex = PassErrorHandler.handle(response)
        Assert.assertEquals(404, ex.code)
        Assert.assertEquals("message", ex.message)
    }

    @Test fun transformResponseWithoutBodyToEx() {
        val response = Response(404)
        val ex = PassErrorHandler.handle(response)
        Assert.assertEquals(404, ex.code)
        Assert.assertEquals(null, ex.message)
    }

    @Test(expected = HttpException::class)
    fun testThrowHttpExceptionIfError() = asyncTest {
        val client = AconiteClient(
                httpClient = TestHttpClient { _, _ -> Response(code = 404) },
                errorHandler = PassErrorHandler
        )
        val api = client.create<TestModuleApi>()
        api.get("foo", "bar")
    }
}