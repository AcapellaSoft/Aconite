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
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Vertx http client.
 * Can process requests in many connections.
 * @param[connectionsCount] number of connections
 */
class VertxHttpClient(
        private val connectionsCount: Int = 1,
        private val vertx: Vertx = Vertx.vertx()
) : io.aconite.client.HttpClient {
    private val clients = (1..connectionsCount).map { WebClient.create(vertx) }
    private val clientIndex = AtomicInteger()
    private val coroutineCtx = VertxCoroutineContext()

    private inner class VertxCoroutineContext: CoroutineDispatcher() {
        private val ctx = vertx.getOrCreateContext()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            ctx.runOnContext { block.run() }
        }
    }

    suspend override fun makeRequest(url: String, request: Request) = suspendCancellableCoroutine<Response> { c ->
        val handler = Handler<AsyncResult<HttpResponse<Buffer>>> { response ->
            handleResponse(c, response)
        }
        sendRequest(url, request, handler)
    }

    private fun selectClient(): WebClient {
        val index = clientIndex.incrementAndGet() % connectionsCount
        return clients[index]
    }

    private fun sendRequest(url: String, request: Request, handler: Handler<AsyncResult<HttpResponse<Buffer>>>) {
        val method = HttpMethod.valueOf(request.method)
        val client = selectClient()
        async(coroutineCtx) {
            client.requestAbs(method, url).apply {
                request.body?.contentType?.let { putHeader("Content-Type", it) }
                putHeader("Accept", "*/*")

                for ((name, value) in request.headers)
                    putHeader(name, value)
                for ((name, value) in request.query)
                    addQueryParam(name, value)

                val body = request.body?.let { Buffer.buffer(it.content.bytes) }
                if (body != null)
                    sendBuffer(body, handler)
                else
                    send(handler)
            }
        }
    }

    private fun handleResponse(continuation: Continuation<Response>, vertxResponse: AsyncResult<HttpResponse<Buffer>>) {
        if (vertxResponse.succeeded()) {
            val result = vertxResponse.result()
            val headers = result.headers()
                    .map { Pair(it.key, it.value) }
                    .toMap()
            val body = result.body()?.let {
                BodyBuffer(
                        content = io.aconite.Buffer.wrap(it.bytes),
                        contentType = result.getHeader("Content-Type") ?: ""
                )
            }

            val response = Response(
                    code = result.statusCode(),
                    headers = headers,
                    body = body
            )
            continuation.resume(response)

        } else {
            continuation.resumeWithException(vertxResponse.cause())
        }
    }
}