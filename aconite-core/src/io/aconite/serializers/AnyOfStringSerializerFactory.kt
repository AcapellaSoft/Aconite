package io.aconite.serializers

import io.aconite.StringSerializer
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

class AnyOfStringSerializerFactory(vararg val serializers: StringSerializer.Factory): StringSerializer.Factory {
    override fun create(annotations: KAnnotatedElement, type: KType) = serializers
            .asSequence()
            .map { it.create(annotations, type) }
            .firstOrNull { it != null }
}

fun oneOf(vararg serializers: StringSerializer.Factory) = AnyOfStringSerializerFactory(*serializers)