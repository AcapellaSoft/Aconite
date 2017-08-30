package io.aconite

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType

/**
 * Used to serialize and deserialize object to [Buffer].
 */
interface BodySerializer {
    /**
     * Used to create [BodySerializer] for concrete type.
     */
    interface Factory {
        /**
         * Creates [BodySerializer] for [type]. If this serializer does not support
         * [type], then it must return `null`.
         * @param[annotations] annotations of function, field or argument to be serialized
         * @param[type] type of object to be serialized
         * @return serializer for [type] or `null` if [type] is not supported
         */
        fun create(annotations: KAnnotatedElement, type: KType): BodySerializer?
    }

    /**
     * Serializes [obj] to [Buffer]. The [obj] must be of type, that was passed
     * to [Factory.create] function.
     * @param[obj] to be serialized
     * @return serialized body
     */
    fun serialize(obj: Any?): Buffer

    /**
     * Deserializes [body] to object with type, that was passed to [Factory.create] function.
     * @param[body] to be deserialized
     * @return deserialized object
     * @throws[BadRequestException] if [body] content can not be deserialized
     */
    fun deserialize(body: Buffer): Any?
}