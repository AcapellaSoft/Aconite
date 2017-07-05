package io.aconite

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

interface Buffer {
    val string: String
    val bytes: ByteArray

    companion object Factory {
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
        body = BodyBuffer(
                content = Buffer.wrap(this.message ?: ""),
                contentType = "text/plain"
        )
)