package io.aconite.server.serializers

import io.aconite.server.StringSerializer
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

class AnyOfStringSerializerFactory(vararg val serializers: StringSerializer.Factory): StringSerializer.Factory {
    override fun create(annotations: KAnnotatedElement, type: KType) = serializers
            .map { it.create(annotations, type) }
            .firstOrNull { it != null }
}

fun anyOf(vararg serializers: StringSerializer.Factory) = AnyOfStringSerializerFactory(*serializers)