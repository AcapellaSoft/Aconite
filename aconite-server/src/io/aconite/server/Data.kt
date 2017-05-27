package io.aconite.server

import java.nio.Buffer

data class Request (
        val method: String,
        val path: Map<String, String> = emptyMap(),
        val query: Map<String, String> = emptyMap(),
        val headers: Map<String, String> = emptyMap(),
        val body: BodyBuffer? = null
)

data class Response (
        val headers: Map<String, String> = emptyMap(),
        val body: BodyBuffer? = null
)

data class BodyBuffer(
        val content: Buffer,
        val contentType: String
)