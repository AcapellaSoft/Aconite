package io.aconite.server.handlers

import io.aconite.Buffer
import io.aconite.Request
import io.aconite.Response
import io.aconite.server.AconiteServer
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

class VertxHandler(private val server: AconiteServer): Handler<RoutingContext> {
    companion object {
        fun runServer(server: AconiteServer, port: Int) {
            val vertx = Vertx.vertx()
            val router = Router.router(vertx)
            val handler = VertxHandler(server)
            router.route().handler(BodyHandler.create())
            router.route().handler(handler)
            vertx.createHttpServer()
                    .requestHandler(router::accept)
                    .listen(port)
        }
    }

    class VertxCoroutineContext(vertx: Vertx): CoroutineDispatcher() {
        private val ctx = vertx.getOrCreateContext()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            ctx.runOnContext { block.run() }
        }
    }

    private class VertxBuffer(private val buffer: io.vertx.core.buffer.Buffer): Buffer {
        override val bytes by lazy { buffer.bytes!! }
        override val string by lazy { buffer.toString() }
    }

    override fun handle(routingCtx: RoutingContext) {
        async(server.coroutineContext) {
            try {
                val request = makeRequest(routingCtx)
                val url = routingCtx.request().uri().substringBefore('?')
                val response = server.accept(url, request)
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
                body = VertxBuffer(ctx.body)
        )
    }

    private fun makeResponse(ctx: RoutingContext, response: Response) {
        ctx.response().apply {
            for ((k, v) in response.headers)
                putHeader(k, v)
            statusCode = response.code

            launch(server.coroutineContext) {
                if (response.isChunked) {
                    isChunked = true
                    for (part in response.body)
                        write(io.vertx.core.buffer.Buffer.buffer(part.bytes))
                    end()
                } else {
                    isChunked = false
                    val body = response.body.receiveOrNull() ?: Buffer.EMPTY
                    end(io.vertx.core.buffer.Buffer.buffer(body.bytes))
                }
            }
        }
    }
}