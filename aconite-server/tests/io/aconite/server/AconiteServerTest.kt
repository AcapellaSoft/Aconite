package io.aconite.server

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

    @Test
    fun testRegisterFailed() = asyncTest {
        val server = AconiteServer(
                bodySerializer = TestBodySerializer.Factory(),
                stringSerializer = TestStringSerializer.Factory(),
                callAdapter = TestCallAdapter(),
                methodFilter = MethodFilterPassAll()
        )
        try {
            server.register(RootModule(), RootModuleApi::class)
            Assert.assertTrue(false)
        } catch (ex: AconiteServerException) {
            Assert.assertTrue(true)
        }
    }
}