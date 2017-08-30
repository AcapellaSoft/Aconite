package io.aconite.client.clients

import io.aconite.Request
import io.aconite.Response
import io.aconite.utils.toChannel
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
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext

class VertxHttpClient(val port: Int, val host: String, private val vertx: Vertx = Vertx.vertx()) : io.aconite.client.HttpClient {
    private val client = WebClient.create(vertx)
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

    private fun sendRequest(url: String, request: Request, handler: Handler<AsyncResult<HttpResponse<Buffer>>>) {
        val method = HttpMethod.valueOf(request.method)
        async(coroutineCtx) {
            client.request(method, port, host, url).apply {
                for ((name, value) in request.headers)
                    putHeader(name, value)
                for ((name, value) in request.query)
                    addQueryParam(name, value)

                val body = request.body?.let { Buffer.buffer(it.bytes) }
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
            val body = result.body()?.let { io.aconite.Buffer.wrap(it.bytes) }

            val response = Response(
                    code = result.statusCode(),
                    headers = headers,
                    body = body.toChannel()
            )
            continuation.resume(response)

        } else {
            continuation.resumeWithException(vertxResponse.cause())
        }
    }
}