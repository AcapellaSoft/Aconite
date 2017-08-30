package io.aconite.serializers

import io.aconite.BodySerializer
import io.aconite.Buffer
import io.aconite.EmptyAnnotations
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
        override fun deserialize(body: Buffer) = throw NotImplementedError()
    }

    @Test fun testFirstAcceptableSelected() {
        val serializer = oneOf(
                TestBodySerializer(String::class),
                GsonBodySerializer.Factory()
        )
        val selected = serializer.create(EmptyAnnotations, String::class.createType())
        Assert.assertEquals(serializer.serializers.first(), selected)
    }

    @Test fun testSelectOtherIfNotAccepted() {
        val serializer = oneOf(
                TestBodySerializer(String::class),
                GsonBodySerializer.Factory()
        )
        val selected = serializer.create(EmptyAnnotations, Long::class.createType())
        Assert.assertNotEquals(serializer.serializers.first(), selected)
    }

    @Test fun testNoOneAccepted() {
        val serializer = oneOf(
                TestBodySerializer(String::class),
                TestBodySerializer(Long::class)
        )
        val selected = serializer.create(EmptyAnnotations, Int::class.createType())
        Assert.assertNull(selected)
    }
}