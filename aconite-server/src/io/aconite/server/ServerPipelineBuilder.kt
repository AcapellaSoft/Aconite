package io.aconite.server

class ServerPipelineBuilder {
    private val handlers = mutableListOf<(ServerRequestAcceptor) -> ServerRequestAcceptor>()

    fun <C> install(factory: ServerRequestAcceptor.Factory<C>, configurator: C.() -> Unit) {
        handlers.add { inner -> factory.create(inner, configurator) }
    }

    fun <C> install(factory: ServerRequestAcceptor.Factory<C>) {
        handlers.add { inner -> factory.create(inner, {}) }
    }

    fun build(): ServerRequestAcceptor {
        val init: ServerRequestAcceptor = NotFoundRequestAcceptor
        return handlers
                .reversed()
                .fold(init) { inner, factory -> factory(inner) }
    }
}

/**
 * Creates server pipeline.
 * Installed acceptors processing request in listed order.
 * Last acceptor is always [NotFoundRequestAcceptor].
 *
 * val server = serverPipeline {
 *     install(First) { param = 123 }
 *     install(Second)
 * }
 *
 * is equivalent to
 *
 * val second = Second.create(NotFoundRequestAcceptor)
 * val first = First.create(second, { param = 123 })
 * val server = first
 *
 * It can be used with AconiteServer like this:
 *
 * val server = serverPipeline {
 *     install(AconiteServer) {
 *         // parameters
 *         bodySerializer = ...
 *         stringSerializer = ...
 *         ...
 *
 *         // handlers
 *         register<FooApi> { FooImpl() }
 *         ...
 *     }
 * }
 */
fun serverPipeline(builder: ServerPipelineBuilder.() -> Unit): ServerRequestAcceptor {
    return ServerPipelineBuilder().apply(builder).build()
}