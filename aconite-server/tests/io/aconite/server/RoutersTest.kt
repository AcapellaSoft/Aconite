package io.aconite.server

import io.aconite.BodyBuffer
import io.aconite.utils.UrlTemplate
import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer

class RoutersTest {

    class TestHandler(override var argsCount: Int, val message: String): AbstractHandler() {
        override fun accept(url: String, request: Request): Response? {
            if (request.path.size < argsCount) return null
            return Response(
                    body = BodyBuffer(ByteBuffer.wrap(message.toByteArray()), "text/plain"),
                    headers = request.path
            )
        }
    }

    class UrlToBodyHandler: AbstractHandler() {
        override val argsCount: Int
            get() = 0

        override fun accept(url: String, request: Request): Response? {
            if (request.path.size < argsCount) return null
            return Response(
                    body = BodyBuffer(ByteBuffer.wrap(url.toByteArray()), "text/plain"),
                    headers = request.path
            )
        }
    }

    fun Response?.body() = String((this?.body?.content as ByteBuffer).array())

    @Test
    fun testSimpleModuleRouter() {
        val handler = TestHandler(0, "from handler")
        val router = ModuleRouter(UrlTemplate("/foo/bar"), listOf(handler))

        val response = router.accept("/foo/bar", Request("GET"))
        val body = response.body()
        Assert.assertEquals("from handler", body)
    }

    @Test
    fun testModuleRouterWithParams() {
        val handler = TestHandler(0, "from handler")
        val router = ModuleRouter(UrlTemplate("/foo/{bar}/baz/{qux}"), listOf(handler))

        val response = router.accept("/foo/12345/baz/67890", Request("GET"))
        val body = response.body()
        Assert.assertEquals("from handler", body)
        Assert.assertEquals("12345", response?.headers?.get("bar"))
        Assert.assertEquals("67890", response?.headers?.get("qux"))
    }

    @Test
    fun testModuleRouterWithParamsNotMatch() {
        val handler = TestHandler(0, "from handler")
        val router = ModuleRouter(UrlTemplate("/foo/{bar}/baz/{qux}"), listOf(handler))

        val response = router.accept("/foo123/12345/baz/67890", Request("GET"))
        Assert.assertNull(response)
    }

    @Test
    fun testModuleRouterMultipleHandlers() {
        val router = ModuleRouter(UrlTemplate("/foo/{bar}/baz/{qux}"), listOf(
                TestHandler(1, "one"),
                TestHandler(2, "two"),
                TestHandler(4, "four")
        ))

        var response: Response?

        response = router.accept("/foo/12345/baz/67890", Request("GET"))
        Assert.assertEquals("two", response.body())

        response = router.accept("/foo/12345/baz/67890", Request("GET", path = mapOf(
                "abc" to "123",
                "qwerty" to "456"
        )))
        Assert.assertEquals("four", response.body())
    }

    @Test
    fun testModuleRouterPartialMatch() {
        val handler = UrlToBodyHandler()
        val router = ModuleRouter(UrlTemplate("/foo/{bar}"), listOf(handler))

        val response = router.accept("/foo/12345/baz/67890", Request("GET"))
        Assert.assertEquals("/baz/67890", response.body())
    }
}