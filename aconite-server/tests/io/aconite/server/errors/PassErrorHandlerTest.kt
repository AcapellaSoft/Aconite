package io.aconite.server.errors

import io.aconite.Buffer
import io.aconite.Request
import io.aconite.server.*
import org.junit.Assert
import org.junit.Test

class PassErrorHandlerTest {
    @Test fun testRethrowException() = asyncTest {
        val server = AconiteServer(errorHandler = PassErrorHandler)
        server.register(ThrowsImpl { RuntimeException(it) }, ThrowsApi::class)
        val response = server.accept("/", Request("GET", body = Buffer.wrap("123")))
        Assert.assertEquals(500, response?.code)
    }
}