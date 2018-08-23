package io.aconite.server.handlers

import io.aconite.BodyBuffer
import io.aconite.Buffer
import io.aconite.Request
import io.aconite.Response
import io.aconite.server.HttpVersion
import io.aconite.server.RequestInfo
import io.aconite.server.ServerRequestAcceptor
import io.aconite.utils.parseContentType
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

class VertxHandler(private val vertx: Vertx, private val acceptor: ServerRequestAcceptor): Handler<RoutingContext> {
    val coroutineCtx : CoroutineContext = VertxCoroutineContext()

    companion object {
        fun runServer(server: ServerRequestAcceptor, port: Int) {
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
        launch(coroutineCtx) {
            try {
                val request = makeRequest(routingCtx)
                val info = RequestInfo(
                        url = routingCtx.request().uri().substringBefore('?'),
                        remoteClient = routingCtx.request().remoteAddress()?.host() ?: "",
                        protocolVersion = routingCtx.request().version().toAconiteVersion()
                )
                val response = acceptor.accept(info, request)
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

    private fun io.vertx.core.http.HttpVersion.toAconiteVersion() = when (this) {
        io.vertx.core.http.HttpVersion.HTTP_1_0 -> HttpVersion.HTTP_1_0
        io.vertx.core.http.HttpVersion.HTTP_1_1 -> HttpVersion.HTTP_1_1
        io.vertx.core.http.HttpVersion.HTTP_2 -> HttpVersion.HTTP_2_0
    }
}