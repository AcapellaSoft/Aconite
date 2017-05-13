package io.aconite

import java.nio.Buffer

data class BodyBuffer(
        val body: Buffer,
        val mediaType: String
)