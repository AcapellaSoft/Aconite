package io.aconite.client.errors

import io.aconite.HttpException
import io.aconite.client.ClientRequestAcceptor

object PassErrorHandler : ClientRequestAcceptor.Factory<Unit> {
    override fun create(inner: ClientRequestAcceptor, configurator: Unit.() -> Unit) = ErrorHandler(inner) { error ->
        HttpException(error.code!!, error.body?.content?.string)
    }
}