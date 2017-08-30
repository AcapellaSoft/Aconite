package io.aconite

import io.aconite.utils.emptyChannel
import io.aconite.utils.toChannel
import kotlinx.coroutines.experimental.channels.ReceiveChannel

data class Request (
        val method: String = "",
        val path: Map<String, String> = emptyMap(),
        val query: Map<String, String> = emptyMap(),
        val headers: Map<String, String> = emptyMap(),
        val body: Buffer? = null
)

data class Response (
        val code: Int = 200,
        val headers: Map<String, String> = emptyMap(),
        val isChunked: Boolean = false,
        val body: ReceiveChannel<Buffer> = emptyChannel()
)

interface Buffer {
    val string: String
    val bytes: ByteArray

    companion object Factory {
        val EMPTY = wrap(ByteArray(0))

        fun wrap(string: String) = object: Buffer {
            override val string = string
            override val bytes by lazy { string.toByteArray() }
        }

        fun wrap(bytes: ByteArray) = object: Buffer {
            override val string by lazy { String(bytes) }
            override val bytes = bytes
        }
    }
}

fun HttpException.toResponse() = Response(
        code = this.code,
        body = Buffer.wrap(this.message ?: "").toChannel()
)