package io.aconite

import io.aconite.BodySerializer.Factory
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

/**
 * Used to serialize and deserialize object to string. String can be
 * header, query or path parameter.
 */
interface StringSerializer {
    /**
     * Used to create [StringSerializer] for concrete type.
     */
    interface Factory {
        /**
         * Creates [StringSerializer] for [type]. If this serializer does not support
         * [type], then it must return `null`.
         * @param[annotations] annotations of function, field or argument to be serialized
         * @param[type] type of object to be serialized
         * @return serializer for [type] or `null` if [type] is not supported
         */
        fun create(annotations: KAnnotatedElement, type: KType): StringSerializer?
    }

    /**
     * Serializes [obj] to string. The [obj] must be of type, that was passed
     * to [Factory.create] function.
     * @param[obj] to be serialized
     * @return serialized string
     */
    fun serialize(obj: Any?): String?

    /**
     * Deserializes [s] to object with type, that was passed to [Factory.create] function.
     * @param[s] to be deserialized
     * @return deserialized object
     * @throws[BadRequestException] if [s] content can not be deserialized
     */
    fun deserialize(s: String): Any?
}