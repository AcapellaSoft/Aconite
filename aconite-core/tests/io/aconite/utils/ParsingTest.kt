package io.aconite.utils

import org.junit.Assert
import org.junit.Test

class ParsingTest {
    @Test
    fun testParseContentTypeWithEncoding() {
        val header = "application/json; encoding=urf-8"
        Assert.assertEquals("application/json", parseContentType(header))
    }

    @Test
    fun testParseContentEmpty() {
        val header = ""
        Assert.assertEquals("", parseContentType(header))
    }

    @Test
    fun parseEmptyQuery() {
        val result = parseQuery("http://localhost:8080/123/foobar")
        Assert.assertEquals("http://localhost:8080/123/foobar", result.url)
        Assert.assertTrue(result.query.isEmpty())
    }

    @Test
    fun parseManyQuery() {
        val result = parseQuery("http://localhost:8080/123/foobar?foo=bar&baz=qux")
        Assert.assertEquals("http://localhost:8080/123/foobar", result.url)
        Assert.assertEquals(mapOf(
                "foo" to "bar",
                "baz" to "qux"
        ), result.query)
    }

    @Test
    fun parseEncodedQuery() {
        val result = parseQuery("http://localhost:8080/123/foobar?baz=qu%20x")
        Assert.assertEquals("http://localhost:8080/123/foobar", result.url)
        Assert.assertEquals(mapOf(
                "baz" to "qu x"
        ), result.query)
    }
}