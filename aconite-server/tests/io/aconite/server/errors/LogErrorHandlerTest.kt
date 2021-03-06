package io.aconite.server.errors

import io.aconite.BodyBuffer
import io.aconite.Buffer
import io.aconite.Request
import io.aconite.server.AconiteServer
import io.aconite.server.RequestInfo
import io.aconite.server.asyncTest
import io.aconite.server.serverPipeline
import org.junit.Assert
import org.junit.Test

class LogErrorHandlerTest {
    @Test fun testConvertToInternalError() = asyncTest {
        val server = serverPipeline {
            install(LogErrorHandler)
            install(AconiteServer) {
                register(ThrowsImpl { RuntimeException(it) }, ThrowsApi::class)
            }
        }
        val response = server.accept(RequestInfo("/"), Request(
                method = "GET",
                body = BodyBuffer(Buffer.wrap("123"), "text/plain")
        ))
        Assert.assertEquals(500, response.code)
    }
}