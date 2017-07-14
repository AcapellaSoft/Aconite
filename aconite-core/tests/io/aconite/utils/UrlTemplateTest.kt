package io.aconite.utils

import org.junit.Assert
import org.junit.Test

class UrlTemplateTest {

    @Test
    fun testTextGreaterThanParam() {
        val a = UrlTemplate("123/foo/text/baz")
        val b = UrlTemplate("123/foo/{bar}/baz")
        Assert.assertTrue(a > b)
    }

    @Test
    fun testParamLessThanText() {
        val a = UrlTemplate("123/foo/{bar}/baz")
        val b = UrlTemplate("123/foo/text/baz")
        Assert.assertTrue(a < b)
    }

    @Test
    fun testMoreSpecificUrlIsGreater() {
        val a = UrlTemplate("123/foo/text/baz")
        val b = UrlTemplate("123/foo/text")
        Assert.assertTrue(a > b)
    }

    @Test
    fun testTwoParamsGreaterThanOne() {
        val a = UrlTemplate("123/{foo}/{bar}")
        val b = UrlTemplate("123/{foo}")
        Assert.assertTrue(a > b)
    }

    @Test
    fun testEmptyLessThanText() {
        val a = UrlTemplate("")
        val b = UrlTemplate("123/bar")
        Assert.assertTrue(a < b)
    }

    @Test
    fun testEmptyLessThanParam() {
        val a = UrlTemplate("")
        val b = UrlTemplate("{baz}")
        Assert.assertTrue(a < b)
    }

    @Test
    fun testSort() {
        val urls = listOf(
                UrlTemplate("/sequences/{name}/items/last"),
                UrlTemplate("/sequences/{name}/items/first"),
                UrlTemplate("/sequences/{name}/items/{key}"),
                UrlTemplate("/sequences/{name}/items"),
                UrlTemplate("/sequences/{name}"),
                UrlTemplate("/sequences")
        )
        Assert.assertEquals(urls.reversed(), urls.sorted())
    }

    @Test(expected = UrlFormatException::class)
    fun testNotValidUrl() {
        UrlTemplate("/foo/{bar{baz}")
    }

    @Test
    fun testPathPartMatch() {
        val template = UrlTemplate("/foo/bar")
        val (rest, _) = template.parse("/foo/bar/baz")!!
        Assert.assertEquals("/baz", rest)
    }

    @Test
    fun testPathFullMatch() {
        val template = UrlTemplate("/foo/bar/baz")
        val params = template.parseEntire("/foo/bar/baz")
        Assert.assertNotNull(params)
    }

    @Test
    fun testPathFullNotMatch() {
        val template = UrlTemplate("/foo/bar")
        val result = template.parseEntire("/foo/bar/baz")
        Assert.assertNull(result)
    }

    @Test
    fun testPathManyArgs() {
        val template = UrlTemplate("/foo/{arg1}/{arg2}/{arg3}")
        val values = template.parseEntire("/foo/bar/baz/123")!!
        Assert.assertEquals(3, values.size)
        Assert.assertEquals("bar", values["arg1"])
        Assert.assertEquals("baz", values["arg2"])
        Assert.assertEquals("123", values["arg3"])
    }

    @Test
    fun testPathEmptyMatch() {
        val template = UrlTemplate("")
        val result = template.parseEntire("/")!!
        Assert.assertNotNull(result)
    }

    @Test
    fun testFormat() {
        val template = UrlTemplate("/foo/{arg1}/bar/{arg2}")
        val url = template.format(mapOf(
                "arg1" to "123",
                "arg2" to "456"
        ))
        Assert.assertEquals("/foo/123/bar/456", url)
    }
}