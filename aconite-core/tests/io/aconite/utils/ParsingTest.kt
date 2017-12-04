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
}