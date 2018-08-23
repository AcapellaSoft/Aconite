package io.aconite.client

import io.aconite.Request
import io.aconite.Response

object NotImplementedRequestAcceptor : ClientRequestAcceptor {
    override suspend fun accept(url: String, request: Request): Response {
        throw NotImplementedError()
    }
}