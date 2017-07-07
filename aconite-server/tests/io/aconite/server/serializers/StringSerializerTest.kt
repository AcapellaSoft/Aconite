package io.aconite.server.serializers

import io.aconite.server.EmptyAnnotations
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.reflect.full.createType

class StringSerializerTest {
    @Test fun testString() {
        val serializer = DefaultStringSerializer.create(EmptyAnnotations, String::class.createType())!!

        Assert.assertEquals("foobar", serializer.serialize("foobar"))
        Assert.assertEquals("foobar", serializer.deserialize("foobar"))
    }

    @Test fun testInt() {
        val serializer = IntegerStringSerializer.create(EmptyAnnotations, Int::class.createType())!!

        Assert.assertEquals("123", serializer.serialize(123))
        Assert.assertEquals(123, serializer.deserialize("123"))
    }

    @Test fun testDouble() {
        val serializer = DoubleStringSerializer.create(EmptyAnnotations, Double::class.createType())!!

        Assert.assertEquals("123.456", serializer.serialize(123.456))
        Assert.assertEquals(123.456, serializer.deserialize("123.456"))
    }

    @Test fun testBoolean() {
        val serializer = BooleanStringSerializer.create(EmptyAnnotations, Boolean::class.createType())!!

        Assert.assertEquals("true", serializer.serialize(true))
        Assert.assertEquals(true, serializer.deserialize("true"))

        Assert.assertEquals("false", serializer.serialize(false))
        Assert.assertEquals(false, serializer.deserialize("false"))
    }

    @Test fun testUUID() {
        val serializer = UuidStringSerializer.create(EmptyAnnotations, UUID::class.createType())!!

        val expected = UUID.randomUUID()
        val string = serializer.serialize(expected)!!
        val actual = serializer.deserialize(string)
        Assert.assertEquals(expected, actual)
        Assert.assertEquals(expected.toString(), string)
    }

    @Test fun testDate() {
        val serializer = DateStringSerializer.create(EmptyAnnotations, Date::class.createType())!!

        val expected = Date()
        val string = serializer.serialize(expected)!!
        val actual = serializer.deserialize(string)
        Assert.assertEquals(expected, actual)
        Assert.assertEquals(expected.toInstant().toEpochMilli().toString(), string)
    }
}