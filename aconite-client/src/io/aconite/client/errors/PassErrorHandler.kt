package io.aconite.client.errors

import io.aconite.HttpException
import io.aconite.Response
import io.aconite.client.ErrorHandler

object PassErrorHandler: ErrorHandler {
    override fun handle(error: Response) = HttpException(error.code, error.body.poll()?.string)
}