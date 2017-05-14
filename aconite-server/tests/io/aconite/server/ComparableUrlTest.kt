package io.aconite.server

import org.junit.Assert
import org.junit.Test

class ComparableUrlTest {

    @Test
    fun testTextGreaterThanParam() {
        val a = ComparableUrl("123/foo/text/baz")
        val b = ComparableUrl("123/foo/{bar}/baz")
        Assert.assertTrue(a > b)
    }

    @Test
    fun testParamLessThanText() {
        val a = ComparableUrl("123/foo/{bar}/baz")
        val b = ComparableUrl("123/foo/text/baz")
        Assert.assertTrue(a < b)
    }

    @Test
    fun testMoreSpecificUrlIsGreater() {
        val a = ComparableUrl("123/foo/text/baz")
        val b = ComparableUrl("123/foo/text")
        Assert.assertTrue(a > b)
    }

    @Test
    fun testTwoParamsGreaterThanOne() {
        val a = ComparableUrl("123/{foo}/{bar}")
        val b = ComparableUrl("123/{foo}")
        Assert.assertTrue(a > b)
    }

    @Test
    fun testEmptyLessThanText() {
        val a = ComparableUrl("")
        val b = ComparableUrl("123/bar")
        Assert.assertTrue(a < b)
    }

    @Test
    fun testEmptyLessThanParam() {
        val a = ComparableUrl("")
        val b = ComparableUrl("{baz}")
        Assert.assertTrue(a < b)
    }

    @Test
    fun testSort() {
        val urls = listOf(
                ComparableUrl("/sequences/{name}/items/last"),
                ComparableUrl("/sequences/{name}/items/first"),
                ComparableUrl("/sequences/{name}/items/{key}"),
                ComparableUrl("/sequences/{name}/items"),
                ComparableUrl("/sequences/{name}"),
                ComparableUrl("/sequences")
        )
        Assert.assertEquals(urls.reversed(), urls.sorted())
    }
}