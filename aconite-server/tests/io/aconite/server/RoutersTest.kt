package io.aconite.server

import io.aconite.HttpError
import io.aconite.utils.UrlTemplate
import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer

private class TestHandler(override var argsCount: Int, val message: String): AbstractHandler() {
    override suspend fun accept(obj: Any, url: String, request: Request): Pair<Response?, HttpError?> {
        if (request.path.size < argsCount) return Pair(null, null)
        return Pair(Response(
                body = BodyBuffer(ByteBuffer.wrap(message.toByteArray()), "text/plain"),
                headers = request.path
        ), null)
    }
}

private class UrlToBodyHandler: AbstractHandler() {
    override val argsCount: Int
        get() = 0

    override suspend fun accept(obj: Any, url: String, request: Request): Pair<Response?, HttpError?> {
        if (request.path.size < argsCount) return Pair(null, null)
        return Pair(Response(
                body = BodyBuffer(ByteBuffer.wrap(url.toByteArray()), "text/plain"),
                headers = request.path
        ), null)
    }
}

class RoutersTest {
    @Test
    fun testSimpleModuleRouter() = asyncTest {
        val handler = TestHandler(0, "from handler")
        val router = ModuleRouter(UrlTemplate("/foo/bar"), listOf(handler))

        val (response, error) = router.accept(Unit, "/foo/bar", Request("GET"))
        Assert.assertNull(error)
        Assert.assertEquals("from handler", response.body())
    }

    @Test
    fun testModuleRouterWithParams() = asyncTest  {
        val handler = TestHandler(0, "from handler")
        val router = ModuleRouter(UrlTemplate("/foo/{bar}/baz/{qux}"), listOf(handler))

        val (response, error) = router.accept(Unit, "/foo/12345/baz/67890", Request("GET"))
        Assert.assertNull(error)
        Assert.assertEquals("from handler", response.body())
        Assert.assertEquals("12345", response?.headers?.get("bar"))
        Assert.assertEquals("67890", response?.headers?.get("qux"))
    }

    @Test
    fun testModuleRouterWithParamsNotMatch() = asyncTest  {
        val handler = TestHandler(0, "from handler")
        val router = ModuleRouter(UrlTemplate("/foo/{bar}/baz/{qux}"), listOf(handler))

        val (response, error) = router.accept(Unit, "/foo123/12345/baz/67890", Request("GET"))
        Assert.assertNull(error)
        Assert.assertNull(response)
    }

    @Test
    fun testModuleRouterMultipleHandlers() = asyncTest  {
        val router = ModuleRouter(UrlTemplate("/foo/{bar}/baz/{qux}"), listOf(
                TestHandler(1, "one"),
                TestHandler(2, "two"),
                TestHandler(4, "four")
        ))

        var response: Response?

        response = router.accept(Unit, "/foo/12345/baz/67890", Request("GET")).first
        Assert.assertEquals("two", response.body())

        response = router.accept(Unit, "/foo/12345/baz/67890", Request("GET", path = mapOf(
                "abc" to "123",
                "qwerty" to "456"
        ))).first
        Assert.assertEquals("four", response.body())
    }

    @Test
    fun testModuleRouterPartialMatch() = asyncTest  {
        val handler = UrlToBodyHandler()
        val router = ModuleRouter(UrlTemplate("/foo/{bar}"), listOf(handler))

        val (response, error) = router.accept(Unit, "/foo/12345/baz/67890", Request("GET"))
        Assert.assertNull(error)
        Assert.assertEquals("/baz/67890", response.body())
    }
}