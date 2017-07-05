package io.aconite.server.serializers

import io.aconite.BodyBuffer
import io.aconite.server.BodySerializer
import io.aconite.server.EmptyAnnotations
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

class AnyOfBodySerializerTest {

    class TestBodySerializer(val cls: KClass<*>) : BodySerializer.Factory, BodySerializer {

        override fun create(annotations: KAnnotatedElement, type: KType): BodySerializer? {
            if (type.classifier != cls) return null
            return this
        }

        override fun serialize(obj: Any?) = throw NotImplementedError()
        override fun deserialize(body: BodyBuffer) = throw NotImplementedError()
    }

    @Test fun testFirstAcceptableSelected() {
        val serializer = anyOf(
                TestBodySerializer(String::class),
                GsonBodySerializer.Factory()
        )
        val selected = serializer.create(EmptyAnnotations, String::class.createType())
        Assert.assertEquals(serializer.serializers.first(), selected)
    }

    @Test fun testSelectOtherIfNotAccepted() {
        val serializer = anyOf(
                TestBodySerializer(String::class),
                GsonBodySerializer.Factory()
        )
        val selected = serializer.create(EmptyAnnotations, Long::class.createType())
        Assert.assertNotEquals(serializer.serializers.first(), selected)
    }

    @Test fun testNoOneAccepted() {
        val serializer = anyOf(
                TestBodySerializer(String::class),
                TestBodySerializer(Long::class)
        )
        val selected = serializer.create(EmptyAnnotations, Int::class.createType())
        Assert.assertNull(selected)
    }
}