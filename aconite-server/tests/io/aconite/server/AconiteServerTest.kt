package io.aconite.server

import io.aconite.AconiteException
import io.aconite.Request
import org.junit.Assert
import org.junit.Test

class AconiteServerTest {

    @Test
    fun testRegisterAccepted() = asyncTest {
        val server = AconiteServer(
                bodySerializer = TestBodySerializer.Factory(),
                stringSerializer = TestStringSerializer.Factory(),
                callAdapter = TestCallAdapter(),
                methodFilter = MethodFilterPassSpecified("get", "post", "test", "patch")
        )
        server.register(RootModule(), RootModuleApi::class)
        val response = server.accept("/foo/bar/kv/keys/abc", Request(
                method = "GET",
                query = mapOf("version" to "123")
        ))
        Assert.assertEquals("key = abc, version = 123, opt = foobar, body = null", response.body())
    }

    @Test
    fun testRegisterNotAccepted() = asyncTest {
        val server = AconiteServer(
                bodySerializer = TestBodySerializer.Factory(),
                stringSerializer = TestStringSerializer.Factory(),
                callAdapter = TestCallAdapter(),
                methodFilter = MethodFilterPassSpecified("get", "post", "test", "patch")
        )
        server.register(RootModule(), RootModuleApi::class)
        val response = server.accept("/foo/bar/kv/keys/abc", Request(
                method = "DELETE",
                query = mapOf("version" to "123")
        ))
        Assert.assertEquals(405, response?.code)
    }

    @Test(expected = AconiteException::class)
    fun testRegisterFailed() = asyncTest {
        val server = AconiteServer(
                bodySerializer = TestBodySerializer.Factory(),
                stringSerializer = TestStringSerializer.Factory(),
                callAdapter = TestCallAdapter(),
                methodFilter = MethodFilterPassAll()
        )
        server.register(RootModule(), RootModuleApi::class)
    }

    @Test
    fun testRegisterFactory() = asyncTest {
        val server = AconiteServer(
                bodySerializer = TestBodySerializer.Factory(),
                stringSerializer = TestStringSerializer.Factory(),
                callAdapter = TestCallAdapter(),
                methodFilter = MethodFilterPassSpecified("get", "post", "test", "patch")
        )

        var counter = 0

        server.register<RootModuleApi> {
            ++counter
            RootModule()
        }

        for (i in 1..2) {
            server.accept("/foo/bar/kv/keys/abc", Request(
                    method = "GET",
                    query = mapOf("version" to "123")
            ))
        }

        Assert.assertEquals(2, counter)
    }
}