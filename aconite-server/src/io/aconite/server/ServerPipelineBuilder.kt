package io.aconite.server

import io.aconite.RequestAcceptor

class ServerPipelineBuilder {
    private val handlers = mutableListOf<(RequestAcceptor) -> RequestAcceptor>()

    fun <C> install(factory: RequestAcceptor.Factory<C>, configurator: C.() -> Unit) {
        handlers.add { inner -> factory.create(inner, configurator) }
    }

    fun <C> install(factory: RequestAcceptor.Factory<C>) {
        handlers.add { inner -> factory.create(inner, {}) }
    }

    fun build(): RequestAcceptor {
        val init: RequestAcceptor = NotFoundRequestAcceptor
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
fun serverPipeline(builder: ServerPipelineBuilder.() -> Unit): RequestAcceptor {
    return ServerPipelineBuilder().apply(builder).build()
}