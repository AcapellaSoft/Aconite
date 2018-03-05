package io.aconite.server.handlers

import io.aconite.*
import io.aconite.utils.parseContentType
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.async
import kotlin.coroutines.experimental.CoroutineContext

class VertxHandler(private val vertx: Vertx, private val acceptor: RequestAcceptor): Handler<RoutingContext> {
    val coroutineCtx : CoroutineContext = VertxCoroutineContext()

    companion object {
        fun runServer(server: RequestAcceptor, port: Int) {
            val vertx = Vertx.vertx()
            val router = Router.router(vertx)
            val handler = VertxHandler(vertx, server)
            router.route().handler(BodyHandler.create())
            router.route().handler(handler)
            vertx.createHttpServer()
                    .requestHandler(router::accept)
                    .listen(port)
        }
    }

    private inner class VertxCoroutineContext: CoroutineDispatcher() {
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
        async(coroutineCtx) {
            try {
                val request = makeRequest(routingCtx)
                val url = routingCtx.request().uri().substringBefore('?')
                val response = acceptor.accept(url, request)
                makeResponse(routingCtx, response)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private fun makeRequest(ctx: RoutingContext): Request {
        val contentType = ctx.request().getHeader("Content-Type")?.let { parseContentType(it) } ?: ""

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
                        contentType = contentType
                )
        )
    }

    private fun makeResponse(ctx: RoutingContext, response: Response) {
        ctx.response().apply {
            for ((k, v) in response.headers)
                putHeader(k, v)
            statusCode = response.code ?: 200

            val body = response.body
            if (body != null) {
                putHeader("Content-Type", body.contentType)
                end(io.vertx.core.buffer.Buffer.buffer(body.content.bytes))
            } else {
                end()
            }
        }
    }
}