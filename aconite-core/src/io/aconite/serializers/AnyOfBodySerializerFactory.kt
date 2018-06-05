package io.aconite.serializers

import io.aconite.BodySerializer
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

class AnyOfBodySerializerFactory(vararg val serializers: BodySerializer.Factory): BodySerializer.Factory {
    override fun create(annotations: KAnnotatedElement, type: KType) = serializers
            .asSequence()
            .map { it.create(annotations, type) }
            .firstOrNull { it != null }
}

fun oneOf(vararg serializers: BodySerializer.Factory) = AnyOfBodySerializerFactory(*serializers)