package io.aconite.server.errors

import io.aconite.server.*
import org.junit.Assert
import org.junit.Test

class LogErrorHandlerTest {
    @Test fun testConvertToInternalError() = asyncTest {
        val server = AconiteServer(errorHandler = LogErrorHandler)
        server.register(ThrowsImpl { RuntimeException(it) }, ThrowsApi::class)
        val response = server.accept("/", Request(
                method = "GET",
                body = BodyBuffer(Buffer.wrap("123"), "text/plain")
        ))
        Assert.assertEquals(500, response?.code)
    }
}