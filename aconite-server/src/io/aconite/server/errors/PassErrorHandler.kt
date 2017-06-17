package io.aconite.server.errors

import io.aconite.server.ErrorHandler

object PassErrorHandler: ErrorHandler {
    override fun handle(ex: Throwable) = null
}