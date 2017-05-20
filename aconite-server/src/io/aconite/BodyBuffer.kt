package io.aconite

import java.nio.Buffer

data class BodyBuffer(
        val content: Buffer,
        val contentType: String
)