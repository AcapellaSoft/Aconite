package io.aconite.server.errors

import io.aconite.BodyBuffer
import io.aconite.Buffer
import io.aconite.Request
import io.aconite.server.*
import org.junit.Assert
import org.junit.Test

class PassErrorHandlerTest {
    @Test fun testRethrowException() = asyncTest {
        val server = AconiteServer(errorHandler = PassErrorHandler)
        server.register(ThrowsImpl { RuntimeException(it) }, ThrowsApi::class)
        val response = server.accept("/", Request("GET", body = BodyBuffer(Buffer.wrap("123"), "text/plain")))
        Assert.assertEquals(500, response?.code)
    }
}