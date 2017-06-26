package io.aconite.server.filters

import org.junit.Assert
import org.junit.Test

class AnnotationMethodFilterTest {
    annotation class TestAnnotation

    @TestAnnotation
    fun annotated() = Unit

    fun notAnnotated() = Unit

    @Test fun testPassesAnnotated() {
        val filter = AnnotationMethodFilter(TestAnnotation::class)
        Assert.assertTrue(filter.predicate(this::annotated))
    }

    @Test fun testRejectNotAnnotated() {
        val filter = AnnotationMethodFilter(TestAnnotation::class)
        Assert.assertFalse(filter.predicate(this::notAnnotated))
    }
}