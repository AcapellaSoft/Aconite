package io.aconite.client.errors

import io.aconite.HttpException
import io.aconite.Response
import io.aconite.client.*
import org.junit.Assert
import org.junit.Test

class PassErrorHandlerTest {
    @Test fun transformResponseToEx() {
        val response = Response(404, body = body("message"))
        val handler = PassErrorHandler.create(ClientRequestAcceptor { _, _ -> response }, {})
        val ex = handler.handle(response)
        Assert.assertEquals(404, ex.code)
        Assert.assertEquals("message", ex.message)
    }

    @Test fun transformResponseWithoutBodyToEx() {
        val response = Response(404)
        val handler = PassErrorHandler.create(ClientRequestAcceptor { _, _ -> response }, {})
        val ex = handler.handle(response)
        Assert.assertEquals(404, ex.code)
        Assert.assertNull(ex.message)
    }

    @Test(expected = HttpException::class)
    fun testThrowHttpExceptionIfError() = asyncTest {
        val pipeline = clientPipeline {
            install(PassErrorHandler)
            install(TestHttpClient { _, _ -> Response(code = 404) })
        }
        val client = AconiteClient(acceptor = pipeline)
        val api = client.create<TestModuleApi>()["http://localhost"]
        api.get("foo", "bar")
    }
}