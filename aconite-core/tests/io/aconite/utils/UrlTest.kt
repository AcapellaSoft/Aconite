package io.aconite.utils

import org.junit.Assert
import org.junit.Test

class UrlTest {

    @Test
    fun formattedUrlNotChanges() {
        val url = "/foo/bar"
        val formatted = formatUrl(url)
        Assert.assertEquals(url, formatted)
    }

    @Test
    fun addSlashToStart() {
        val url = "foo/bar"
        val formatted = formatUrl(url)
        Assert.assertEquals("/foo/bar", formatted)
    }

    @Test
    fun removeSlashFromEnd() {
        val url = "/foo/bar/"
        val formatted = formatUrl(url)
        Assert.assertEquals("/foo/bar", formatted)
    }
}