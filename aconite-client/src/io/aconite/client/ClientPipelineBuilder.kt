package io.aconite.client

class ClientPipelineBuilder {
    private val handlers = mutableListOf<(ClientRequestAcceptor) -> ClientRequestAcceptor>()

    fun <C> install(factory: ClientRequestAcceptor.Factory<C>, configurator: C.() -> Unit) {
        handlers.add { inner -> factory.create(inner, configurator) }
    }

    fun <C> install(factory: ClientRequestAcceptor.Factory<C>) {
        handlers.add { inner -> factory.create(inner, {}) }
    }

    fun build(): ClientRequestAcceptor {
        val init: ClientRequestAcceptor = NotImplementedRequestAcceptor
        return handlers
                .reversed()
                .fold(init) { inner, factory -> factory(inner) }
    }
}

/**
 * Creates client pipeline.
 * Installed acceptors processing request in listed order.
 * Last acceptor is always [NotImplementedRequestAcceptor].
 *
 * val pipeline = clientPipeline {
 *     install(First) { param = 123 }
 *     install(Second)
 * }
 *
 * is equivalent to
 *
 * val second = Second.create(NotImplementedRequestAcceptor)
 * val first = First.create(second, { param = 123 })
 * val pipeline = first
 *
 * It can be used with AconiteClient like this:
 *
 * val pipeline = clientPipeline {
 *     install(ErrorHandler)
 *     install(VertxHttpClient)
 * }
 * val client = AconiteClient(acceptor = pipeline)
 */
fun clientPipeline(builder: ClientPipelineBuilder.() -> Unit): ClientRequestAcceptor {
    return ClientPipelineBuilder().apply(builder).build()
}