package io.aconite.server.serializers

import io.aconite.server.EmptyAnnotations
import io.aconite.server.StringSerializer
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType

class AnyOfStringSerializerTest {

    class TestStringSerializer(val cls: KClass<*>) : StringSerializer.Factory, StringSerializer {

        override fun create(annotations: KAnnotatedElement, type: KType): StringSerializer? {
            if (type.classifier != cls) return null
            return this
        }

        override fun serialize(obj: Any?) = throw NotImplementedError()
        override fun deserialize(s: String) = throw NotImplementedError()
    }

    @Test fun testFirstAcceptableSelected() {
        val serializer = anyOf(
                TestStringSerializer(String::class),
                SimpleStringSerializerFactory()
        )
        val selected = serializer.create(EmptyAnnotations, String::class.createType())
        Assert.assertEquals(serializer.serializers.first(), selected)
    }

    @Test fun testSelectOtherIfNotAccepted() {
        val serializer = anyOf(
                TestStringSerializer(String::class),
                SimpleStringSerializerFactory()
        )
        val selected = serializer.create(EmptyAnnotations, Long::class.createType())
        Assert.assertNotEquals(serializer.serializers.first(), selected)
    }

    @Test fun testNoOneAccepted() {
        val serializer = anyOf(
                TestStringSerializer(String::class),
                TestStringSerializer(Long::class)
        )
        val selected = serializer.create(EmptyAnnotations, Int::class.createType())
        Assert.assertNull(selected)
    }
}