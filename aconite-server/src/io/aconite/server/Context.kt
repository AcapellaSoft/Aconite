package io.aconite.server

import io.aconite.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Contains reference to the [response] object. Only way to mutate the response is
 * to copy original response and set reference to new response.
 */
class CoroutineResponseReference(var response: Response) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<CoroutineResponseReference>

    override val key: CoroutineContext.Key<*> = Key
}

enum class SameSiteType {
    STRICT,
    LAX
}

/**
 * Helper object for common operations with response context.
 * Response context is write only.
 */
object response {
    private val expireDateFormat = SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz", Locale.ROOT)

    /**
     * Set header [key] of the response to [value].
     * Inner modules can overwrite value of this header.
     */
    suspend fun putHeader(key: String, value: String) = suspendCoroutine<Unit> { cont ->
        val response = cont.context[CoroutineResponseReference]!!
        response.response = response.response.copy(
                headers = response.response.headers + (key to value)
        )
        cont.resume(Unit)
    }

    /**
     * Set status code of the response to [value].
     * Inner modules can overwrite status code.
     */
    suspend fun setStatusCode(value: Int) = suspendCoroutine<Unit> { cont ->
        val response = cont.context[CoroutineResponseReference]!!
        response.response = response.response.copy(
                code = value
        )
        cont.resume(Unit)
    }

    /**
     * Serialize and put value to Set-Cookie header.
     * @param[cookies] map of cookie values
     * @param[expires] the maximum lifetime of the cookie as a [Date]
     * @param[maxAge] number of seconds until the cookie expires
     * @param[domain] specifies those hosts to which the cookie will be sent
     * @param[path] indicates a URL path that must exist in the requested resource
     * before sending the cookie header
     * @param[sameSite] allows servers to assert that a cookie ought not to be sent
     * along with cross-site requests, which provides some protection against cross-site
     * request forgery attacks
     * @param[secure] a secure cookie will only be sent to the server when a request
     * is made using SSL and the HTTPS protocol
     * @param[httpOnly] HTTP-only cookies aren't accessible via JavaScript
     */
    suspend fun setCookie(
            cookies: Map<String, String>,
            expires: Date? = null,
            maxAge: Long? = null,
            domain: String? = null,
            path: String? = null,
            sameSite: SameSiteType? = null,
            secure: Boolean = false,
            httpOnly: Boolean = false
    ) {
        val cookie = buildString {
            cookies.forEach { k, v -> appendToCookie(k, v) }
            appendToCookie("Expires", expires?.let { expireDateFormat.format(it) })
            appendToCookie("MaxAge", maxAge?.toString())
            appendToCookie("Domain", domain)
            appendToCookie("Path", path)
            appendToCookie("SameSite", sameSite?.toString())
            if (secure) append("Secure; ")
            if (httpOnly) append("HttpOnly; ")
        }
        putHeader("Set-Cookie", cookie)
    }

    private fun StringBuilder.appendToCookie(key: String, value: String?) {
        if (value != null) {
            append(key)
            append('=')
            append(value)
            append("; ")
        }
    }
}