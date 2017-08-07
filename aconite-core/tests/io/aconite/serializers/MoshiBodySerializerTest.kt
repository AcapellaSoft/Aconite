package io.aconite.serializers

import io.aconite.BadRequestException
import io.aconite.BodyBuffer
import io.aconite.Buffer
import io.aconite.EmptyAnnotations
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class MoshiBodySerializerTest {
    data class Foo<out T: Any>(
            val bar: T,
            val baz: String,
            val qux: String
    )

    data class Bar(
            val a: Int,
            val b: Int
    )

    @Test fun testIntType() {
        val serializer = MoshiBodySerializer.Factory().create(EmptyAnnotations, Int::class.createType())
        Assert.assertEquals("123", serializer.serialize(123).content.string)
        Assert.assertEquals(123, serializer.deserialize(BodyBuffer(Buffer.wrap("123"), "application/json")))
    }

    @Test fun testLongType() {
        val serializer = MoshiBodySerializer.Factory().create(EmptyAnnotations, Long::class.createType())
        Assert.assertEquals("123", serializer.serialize(123L).content.string)
        Assert.assertEquals(123L, serializer.deserialize(BodyBuffer(Buffer.wrap("123"), "application/json")))
    }

    @Test fun testComplexGenericType() {
        // FIXME: Not working because of https://github.com/square/moshi/issues/309

        val type = Foo::class.createType(listOf(
                KTypeProjection.invariant(Bar::class.createType())
        ))
        val serializer = MoshiBodySerializer.Factory().create(EmptyAnnotations, type)

        val expected = Foo(bar = Bar(1, 2), baz = "baz", qux = UUID.randomUUID().toString())
        val body = serializer.serialize(expected)
        val actual = serializer.deserialize(body)

        Assert.assertEquals(expected, actual)
    }

    @Test fun testComplexType() {
        val type = Bar::class.createType()
        val serializer = MoshiBodySerializer.Factory().create(EmptyAnnotations, type)

        val expected = Bar(1, 2)
        val body = serializer.serialize(expected)
        val actual = serializer.deserialize(body)

        Assert.assertEquals(expected, actual)
    }

    @Test(expected = BadRequestException::class) fun testMalformedBodyRequest() {
        val type = Bar::class.createType()
        val serializer = MoshiBodySerializer.Factory().create(EmptyAnnotations, type)

        val bodyWithError = BodyBuffer(
                content = Buffer.wrap("!@^%"),
                contentType = "application/json"
        )
        serializer.deserialize(bodyWithError)
    }

    @Test(expected = BadRequestException::class) fun testMissingField() {
        val type = Bar::class.createType()
        val serializer = MoshiBodySerializer.Factory().create(EmptyAnnotations, type)

        val bodyWithError = BodyBuffer(
                content = Buffer.wrap("{\"a\":100}"),
                contentType = "application/json"
        )
        serializer.deserialize(bodyWithError)
    }

    @Test fun testNull() {
        val serializer = MoshiBodySerializer.Factory().create(EmptyAnnotations, Bar::class.createType())

        val expected: Bar? = null
        val body = serializer.serialize(expected)
        val actual = serializer.deserialize(body)

        Assert.assertEquals(expected, actual)
    }
}