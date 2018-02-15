package io.aconite.client.errors

import io.aconite.HttpException
import io.aconite.RequestAcceptor

object PassErrorHandler : RequestAcceptor.Factory<Unit> {
    override fun create(inner: RequestAcceptor, configurator: Unit.() -> Unit) = ErrorHandler(inner) { error ->
        HttpException(error.code!!, error.body?.content?.string)
    }
}