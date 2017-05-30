package io.aconite.server

import kotlinx.coroutines.experimental.future.future
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KCallable
import kotlin.reflect.KType

class TestBodySerializer: BodySerializer {

    class Factory: BodySerializer.Factory {
        override fun create(annotations: KAnnotatedElement, type: KType)
                = if (type.classifier == String::class) TestBodySerializer() else null
    }

    override fun serialize(obj: Any?): BodyBuffer {
        return BodyBuffer(ByteBuffer.wrap((obj as String).toByteArray()), "plain/text")
    }

    override fun deserialize(body: BodyBuffer): Any? {
        return String((body.content as ByteBuffer).array())
    }
}

class TestStringSerializer: StringSerializer {

    class Factory: StringSerializer.Factory {
        override fun create(annotations: KAnnotatedElement, type: KType)
                = if (type.classifier == String::class) TestStringSerializer() else null
    }

    override fun serialize(obj: Any?): String {
        return obj as String
    }

    override fun deserialize(s: String): Any? {
        return s
    }
}

class TestCallAdapter: CallAdapter {
    override fun adapt(fn: KCallable<*>) = fn
}

class TestMethodFilter: MethodFilter {
    override fun predicate(fn: KCallable<*>) = true

}

fun Response?.body() = String((this?.body?.content as ByteBuffer).array())

fun body(s: String) = BodyBuffer(ByteBuffer.wrap(s.toByteArray()), "text/plain")

fun asyncTest(timeout: Long = 1, unit: TimeUnit = TimeUnit.SECONDS, block: suspend () -> Unit)
        = future { block() }.get(timeout, unit)!!
