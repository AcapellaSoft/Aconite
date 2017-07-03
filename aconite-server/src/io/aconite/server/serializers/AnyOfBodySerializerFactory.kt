package io.aconite.server.serializers

import io.aconite.server.BodySerializer
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

class AnyOfBodySerializerFactory(vararg val serializers: BodySerializer.Factory): BodySerializer.Factory {
    override fun create(annotations: KAnnotatedElement, type: KType) = serializers
            .map { it.create(annotations, type) }
            .firstOrNull { it != null }
}

fun anyOf(vararg serializers: BodySerializer.Factory) = AnyOfBodySerializerFactory(*serializers)