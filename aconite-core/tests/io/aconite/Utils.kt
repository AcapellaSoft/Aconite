package io.aconite

import kotlin.reflect.KAnnotatedElement

object EmptyAnnotations: KAnnotatedElement {
    override val annotations: List<Annotation> get() = emptyList()
}