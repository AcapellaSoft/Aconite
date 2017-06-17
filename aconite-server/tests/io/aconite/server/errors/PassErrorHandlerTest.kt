package io.aconite.server.errors

import io.aconite.annotations.Body
import io.aconite.annotations.GET
import io.aconite.server.*
import org.junit.Assert
import org.junit.Test

class PassErrorHandlerTest {
    @Test fun testRethrowException() = asyncTest {
        val server = AconiteServer(errorHandler = PassErrorHandler)
        server.register(ThrowsImpl(), ThrowsApi::class)
        try {
            server.accept("/", Request("GET", body = BodyBuffer(Buffer.wrap("123"), "text/plain")))
            Assert.assertTrue(false)
        } catch (ex: RuntimeException) {
            Assert.assertEquals("123", ex.message)
        }
    }
}