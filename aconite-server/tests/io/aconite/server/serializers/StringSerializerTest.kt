package io.aconite.server.serializers

import io.aconite.server.EmptyAnnotations
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.reflect.full.createType

class StringSerializerTest {
    @Test fun testString() {
        val serializer = SimpleStringSerializerFactory().create(EmptyAnnotations, String::class.createType())!!

        Assert.assertEquals("foobar", serializer.serialize("foobar"))
        Assert.assertEquals("foobar", serializer.deserialize("foobar"))
    }

    @Test fun testInt() {
        val serializer = SimpleStringSerializerFactory().create(EmptyAnnotations, Int::class.createType())!!

        Assert.assertEquals("123", serializer.serialize(123))
        Assert.assertEquals(123, serializer.deserialize("123"))
    }

    @Test fun testUUID() {
        val serializer = SimpleStringSerializerFactory().create(EmptyAnnotations, UUID::class.createType())!!

        val expected = UUID.randomUUID()
        val string = serializer.serialize(expected)!!
        val actual = serializer.deserialize(string)
        Assert.assertEquals(expected, actual)
        Assert.assertEquals(expected.toString(), string)
    }
}