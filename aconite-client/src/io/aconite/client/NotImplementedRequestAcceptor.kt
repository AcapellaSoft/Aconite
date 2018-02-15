package io.aconite.client

import io.aconite.Request
import io.aconite.RequestAcceptor
import io.aconite.Response

object NotImplementedRequestAcceptor : RequestAcceptor {
    override suspend fun accept(url: String, request: Request): Response {
        throw NotImplementedError()
    }
}