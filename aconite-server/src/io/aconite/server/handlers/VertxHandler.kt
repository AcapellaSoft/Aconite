package io.aconite.server.handlers

import io.aconite.BodyBuffer
import io.aconite.Buffer
import io.aconite.Request
import io.aconite.Response
import io.aconite.server.*
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.async
import kotlin.coroutines.experimental.CoroutineContext

class VertxHandler(private val vertx: Vertx, private val server: AconiteServer): Handler<RoutingContext> {
    val coroutineCtx = VertxCoroutineContext()

    inner class VertxCoroutineContext: CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            vertx.runOnContext { block.run() }
        }
    }

    class VertxBuffer(private val buffer: io.vertx.core.buffer.Buffer): Buffer {
        override val bytes by lazy { buffer.bytes!! }
        override val string by lazy { buffer.toString() }
    }

    override fun handle(routingCtx: RoutingContext) {
        async(coroutineCtx) {
            try {
                val request = makeRequest(routingCtx)
                val response = server.accept(routingCtx.request().uri(), request)
                if (response != null) {
                    makeResponse(routingCtx, response)
                } else {
                    routingCtx.next()
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private fun makeRequest(ctx: RoutingContext): Request {
        return Request(
                method = ctx.request().rawMethod(),
                path = ctx.pathParams(),
                headers = ctx.request().headers()
                        .asSequence()
                        .map { Pair(it.key, it.value) }
                        .toMap(),
                query = ctx.request().params()
                        .asSequence()
                        .map { Pair(it.key, it.value) }
                        .toMap(),
                body = BodyBuffer(
                        content = VertxBuffer(ctx.body),
                        contentType = ctx.request().getHeader("Content-Type") ?: ""
                )
        )
    }

    private fun makeResponse(ctx: RoutingContext, response: Response) {
        ctx.response().apply {
            for ((k, v) in response.headers)
                putHeader(k, v)
            statusCode = response.code
            response.body?.let { putHeader("Content-Type", it.contentType) }
            end(io.vertx.core.buffer.Buffer.buffer(response.body?.content?.bytes))
        }
    }
}