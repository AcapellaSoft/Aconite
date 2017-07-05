package io.aconite.server.adapters

import io.aconite.Request
import io.aconite.server.*
import org.junit.Assert
import org.junit.Test

private fun server() = AconiteServer(
        bodySerializer = TestBodySerializer.Factory(),
        stringSerializer = TestStringSerializer.Factory(),
        callAdapter = anyOf(
                CompletableFutureCallAdapter,
                SuspendCallAdapter
        ),
        methodFilter = MethodFilterPassAll()
)

class AdaptersTest {
    @Test fun testGetSuspend() = asyncTest {
        val server = server()
        server.register(TestModuleMixedCalls(), TestModuleMixedCallsApi::class)
        val response = server.accept("/", Request(
                method = "GET",
                body = body("foobar")
        ))
        Assert.assertEquals("foobar", response.body())
    }

    @Test fun testPostFuture() = asyncTest {
        val server = server()
        server.register(TestModuleMixedCalls(), TestModuleMixedCallsApi::class)
        val response = server.accept("/", Request(
                method = "POST",
                body = body("foobar")
        ))
        Assert.assertEquals("foobar", response.body())
    }
}