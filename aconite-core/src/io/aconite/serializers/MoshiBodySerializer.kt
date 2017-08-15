package io.aconite.serializers

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import io.aconite.*
import io.aconite.utils.toJavaType
import java.lang.reflect.Type
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

class MoshiBodySerializer(moshi: Moshi, type: Type): BodySerializer {
    private val adapter = moshi.adapter<Any?>(type)

    class Factory(val moshi: Moshi): BodySerializer.Factory {

        constructor(builder: Moshi.Builder): this(builder
                .add(KotlinJsonAdapterFactory())
                .build()
        )

        constructor(build: Moshi.Builder.() -> Unit): this(
                Moshi.Builder()
                        .apply(build)
        )

        constructor(): this(Moshi.Builder())

        override fun create(annotations: KAnnotatedElement, type: KType) = MoshiBodySerializer(moshi, type.toJavaType())
    }

    override fun serialize(obj: Any?) = BodyBuffer(
            content = Buffer.wrap(adapter.toJson(obj)),
            contentType = "application/json"
    )

    override fun deserialize(body: BodyBuffer): Any? {
        if (body.content.bytes.isEmpty()) return null

        if (body.contentType.toLowerCase() != "application/json")
            throw UnsupportedMediaTypeException("Only 'application/json' media type supported")

        try {
            return adapter.fromJson(body.content.string)
        } catch (ex: JsonDataException) {
            throw BadRequestException("Bad JSON format. ${ex.message}")
        } catch (ex: JsonEncodingException) {
            throw BadRequestException("Malformed JSON body.")
        }
    }
}