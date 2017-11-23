package io.aconite.bench.vertx

import io.aconite.annotations.GET
import io.aconite.client.AconiteClient
import io.aconite.client.clients.VertxHttpClient
import io.aconite.server.AconiteServer
import io.aconite.server.handlers.VertxHandler
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeoutOrNull
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

interface Api {
    @GET("/test") suspend fun test()
}

class Impl : Api {
    suspend override fun test() {

    }
}

class ServerVerticle(private val host: String, private val port: Int) : AbstractVerticle() {
    override fun start() {
        val server = AconiteServer()
        server.register(Impl(), Api::class)

        val handler = VertxHandler(vertx, server)

        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        router.route().handler(handler)

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(port, host)
    }
}

class ClientVerticle(
        private val host: String,
        private val port: Int,
        private val connections: Int,
        private val counter: AtomicInteger,
        private val timeout: Long
) : AbstractVerticle() {
    override fun start() {
        val client = AconiteClient(
                httpClient = VertxHttpClient(connections, vertx)
        )
        val api = client.create<Api>()["http://$host:$port"]

        (1..connections).forEach {
            launch(Unconfined) {
                while (true) {
                    withTimeoutOrNull(timeout) {
                        api.test()
                        counter.incrementAndGet()
                    }
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    val rand = Random()
    val host = "localhost"
    val port = 30000 + rand.nextInt(30000)
    val connections = 16
    val timeout = 1000L

    val vertx = Vertx.vertx()

    val serverStarted = CompletableFuture<Unit>()
    vertx.deployVerticle(ServerVerticle(host, port)) {
        serverStarted.complete(Unit)
    }
    serverStarted.get()

    println("Server listening on address http://$host:$port")

    val counter = AtomicInteger()
    val clientStated = CompletableFuture<Unit>()
    vertx.deployVerticle(ClientVerticle(host, port, connections, counter, timeout)) {
        clientStated.complete(Unit)
    }
    clientStated.get()

    println("Client started with $connections connections")

    thread {
        while (true) {
            Thread.sleep(1000)
            val rps = counter.getAndSet(0)
            println("$rps requests/sec")
        }
    }
}