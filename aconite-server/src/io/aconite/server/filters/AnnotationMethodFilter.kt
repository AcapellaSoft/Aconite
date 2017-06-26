package io.aconite.server.filters

import io.aconite.server.MethodFilter
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class AnnotationMethodFilter<T: Annotation>(val annotation: KClass<T>): MethodFilter {
    override fun predicate(fn: KFunction<*>) = fn.annotations.any { it.annotationClass == annotation }
}