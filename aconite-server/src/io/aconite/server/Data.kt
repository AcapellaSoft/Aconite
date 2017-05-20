package io.aconite.server

import io.aconite.BodyBuffer

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