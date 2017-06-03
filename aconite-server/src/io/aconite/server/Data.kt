package io.aconite.server

import io.aconite.HttpError
import java.nio.Buffer
import java.nio.ByteBuffer

data class Request (
        val method: String,
        val path: Map<String, String> = emptyMap(),
        val query: Map<String, String> = emptyMap(),
        val headers: Map<String, String> = emptyMap(),
        val body: BodyBuffer? = null
)

data class Response (
        val code: Int = 200,
        val headers: Map<String, String> = emptyMap(),
        val body: BodyBuffer? = null
)

data class BodyBuffer(
        val content: Buffer,
        val contentType: String
)

fun HttpError.toResponse() = Response(
        code = this.code,
        body = BodyBuffer(
                content = ByteBuffer.wrap(this.message.toByteArray()),
                contentType = "text/plain"
        )
)