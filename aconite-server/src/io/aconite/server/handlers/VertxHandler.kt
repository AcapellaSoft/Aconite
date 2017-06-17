package io.aconite.server.handlers

import io.aconite.server.AconiteServer
import io.aconite.server.BodyBuffer
import io.aconite.server.Request
import io.aconite.server.Response
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.async
import java.nio.ByteBuffer
import kotlin.coroutines.experimental.CoroutineContext

class VertxHandler(private val vertx: Vertx, private val server: AconiteServer): Handler<RoutingContext> {
    val coroutineCtx = VertxCoroutineContext()

    inner class VertxCoroutineContext: CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            vertx.runOnContext { block.run() }
        }
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
                        content = ByteBuffer.wrap(ctx.body.bytes),
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
            end(String((response.body?.content as ByteBuffer).array()))
        }
    }
}