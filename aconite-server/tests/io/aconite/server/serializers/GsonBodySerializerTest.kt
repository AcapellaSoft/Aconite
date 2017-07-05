package io.aconite.server.serializers

import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import io.aconite.BodyBuffer
import io.aconite.Buffer
import io.aconite.server.EmptyAnnotations
import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class GsonBodySerializerTest {
    data class Foo<out T>(
            val bar: T,
            val baz: String,
            val qux: UUID
    )

    data class Bar(
            val a: Int,
            val b: Int
    )

    @Test fun testIntType() {
        val serializer = GsonBodySerializer.Factory().create(EmptyAnnotations, Int::class.createType())
        Assert.assertEquals("123", serializer.serialize(123).content.string)
        Assert.assertEquals(123, serializer.deserialize(BodyBuffer(Buffer.wrap("123"), "application/json")))
    }

    @Test fun testLongType() {
        val serializer = GsonBodySerializer.Factory().create(EmptyAnnotations, Long::class.createType())
        Assert.assertEquals("123", serializer.serialize(123L).content.string)
        Assert.assertEquals(123L, serializer.deserialize(BodyBuffer(Buffer.wrap("123"), "application/json")))
    }

    @Test fun testLongTypeWithStringPolicy() {
        val gson = GsonBuilder().setLongSerializationPolicy(LongSerializationPolicy.STRING)
        val serializer = GsonBodySerializer.Factory(gson).create(EmptyAnnotations, Long::class.createType())
        Assert.assertEquals("\"123\"", serializer.serialize(123L).content.string)
        Assert.assertEquals(123L, serializer.deserialize(BodyBuffer(Buffer.wrap("\"123\""), "application/json")))
    }

    @Test fun testComplexType() {
        val type = Foo::class.createType(listOf(
                KTypeProjection.invariant(Bar::class.createType())
        ))
        val serializer = GsonBodySerializer.Factory().create(EmptyAnnotations, type)

        val expected = Foo(bar = Bar(1, 2), baz = "baz", qux = UUID.randomUUID())
        val body = serializer.serialize(expected)
        val actual = serializer.deserialize(body)

        Assert.assertEquals(expected, actual)
    }

    @Test fun testNull() {
        val serializer = GsonBodySerializer.Factory().create(EmptyAnnotations, Bar::class.createType())

        val expected: Bar? = null
        val body = serializer.serialize(expected)
        val actual = serializer.deserialize(body)

        Assert.assertEquals(expected, actual)
    }
}