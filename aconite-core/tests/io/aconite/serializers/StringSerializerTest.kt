package io.aconite.serializers

import io.aconite.BadRequestException
import io.aconite.EmptyAnnotations
import org.junit.Assert
import org.junit.Test
import java.time.Instant
import java.util.*
import kotlin.reflect.full.createType

class StringSerializerTest {
    enum class TestEnum {
        foo,
        bar
    }

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

    @Test fun testNull() {
        val serializer = DefaultStringSerializer.create(EmptyAnnotations, String::class.createType())!!

        val expected = null
        val string = serializer.serialize(expected)
        Assert.assertNull(string)
    }

    @Test fun testEnumSuccess() {
        val serializer = EnumStringSerializer.Factory.create(EmptyAnnotations, TestEnum::class.createType())!!

        val expected = TestEnum.bar
        val string = serializer.serialize(expected)!!
        val actual = serializer.deserialize(string)
        Assert.assertEquals(expected, actual)
        Assert.assertEquals("bar", string)
    }

    @Test(expected = BadRequestException::class)
    fun testEnumFailed() {
        val serializer = EnumStringSerializer.Factory.create(EmptyAnnotations, TestEnum::class.createType())!!
        serializer.deserialize("baz")
    }

    @Test
    fun testCookieDeserialization() {
        val serializer = CookieStringSerializer.create(EmptyAnnotations, Cookie::class.createType())!!

        val expected = Cookie(
                mapOf("foo" to "123", "bar" to "baz"),
                expires = Date.from(Instant.ofEpochSecond(1234567)),
                maxAge = 10,
                domain = "foo-bar.com",
                path = "/abc",
                sameSite = SameSiteType.STRICT,
                secure = true,
                httpOnly = true
        )

        val str = serializer.serialize(expected)!!
        val actual = serializer.deserialize(str) as Cookie

        Assert.assertEquals(expected.data["foo"], actual.data["foo"])
        Assert.assertEquals(expected.data["bar"], actual.data["bar"])
        Assert.assertEquals(expected.expires, actual.expires)
        Assert.assertEquals(expected.maxAge, actual.maxAge)
        Assert.assertEquals(expected.path, actual.path)
        Assert.assertEquals(expected.sameSite, actual.sameSite)
        Assert.assertEquals(expected.secure, actual.secure)
        Assert.assertEquals(expected.httpOnly, actual.httpOnly)
    }
}