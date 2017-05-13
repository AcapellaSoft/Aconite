package io.aconite.server

import io.aconite.Request
import org.junit.Assert
import org.junit.Test

class RequestMatcherTest {

    @Test
    fun testPathPartMatch() {
        val matcher = RequestMatcher.Builder()
                .path("/foo/bar", false)
                .build()
        val result = matcher.match(Request("GET", "/foo/bar/baz"))!!
        Assert.assertEquals("/baz", result.url)
    }

    @Test
    fun testPathFullMatch() {
        val matcher = RequestMatcher.Builder()
                .path("/foo/bar/baz", true)
                .build()
        val result = matcher.match(Request("GET", "/foo/bar/baz"))!!
        Assert.assertEquals("", result.url)
    }

    @Test
    fun testPathFullNotMatch() {
        val matcher = RequestMatcher.Builder()
                .path("/foo/bar", true)
                .build()
        val result = matcher.match(Request("GET", "/foo/bar/baz"))
        Assert.assertNull(result)
    }

    @Test
    fun testPathManyArgs() {
        val matcher = RequestMatcher.Builder()
                .path("/foo/{arg1}/{arg2}/{arg3}", true)
                .build()
        val result = matcher.match(Request("GET", "/foo/bar/baz/123"))!!
        Assert.assertEquals(3, result.path.size)
        Assert.assertEquals("bar", result.path["arg1"])
        Assert.assertEquals("baz", result.path["arg2"])
        Assert.assertEquals("123", result.path["arg3"])
    }

    @Test
    fun testPathEmptyMatch() {
        val matcher = RequestMatcher.Builder()
                .path("", true)
                .build()
        val result = matcher.match(Request("GET", "/"))
        Assert.assertNotNull(result)
    }
}