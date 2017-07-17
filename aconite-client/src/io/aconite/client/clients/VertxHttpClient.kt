package io.aconite.client.clients

import io.aconite.BodyBuffer
import io.aconite.Request
import io.aconite.Response
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import kotlin.coroutines.experimental.suspendCoroutine

class VertxHttpClient(val port: Int, val host: String, vertx: Vertx = Vertx.vertx()) : io.aconite.client.HttpClient {
    private val client = WebClient.create(vertx)

    suspend override fun makeRequest(url: String, request: Request) = suspendCoroutine<Response> { c ->
        val method = HttpMethod.valueOf(request.method)
        val body = request.body?.let { Buffer.buffer(it.content.bytes) }
        val r = client.request(method, port, host, url)
        for ((name, value) in request.headers) r.putHeader(name, value)
        for ((name, value) in request.query) r.addQueryParam(name, value)

        val handler = Handler<AsyncResult<HttpResponse<Buffer>>> { resp ->
            if (resp.succeeded()) {
                val result = resp.result()
                val response = Response(
                        code = result.statusCode(),
                        headers = result.headers()
                                .map { Pair(it.key, it.value) }
                                .toMap(),
                        body = result.body()?.let {
                            BodyBuffer(
                                    content = io.aconite.Buffer.wrap(it.bytes),
                                    contentType = result.getHeader("Content-Type") ?: ""
                            )
                        }
                )
                c.resume(response)
            } else {
                c.resumeWithException(resp.cause())
            }
        }

        if (body != null) r.sendBuffer(body, handler) else r.send(handler)
    }
}