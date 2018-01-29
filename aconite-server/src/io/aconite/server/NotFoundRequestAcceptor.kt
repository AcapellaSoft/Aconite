package io.aconite.server

import io.aconite.BodyBuffer
import io.aconite.Buffer
import io.aconite.Request
import io.aconite.Response

/**
 * Respond with 404 status.
 * Used as last stage in server pipeline, when no previous acceptors has handled request.
 */
object NotFoundRequestAcceptor : RequestAcceptor {
    override suspend fun accept(url: String, request: Request): Response {
        return Response(code = 404, body = BodyBuffer(Buffer.wrap("Resource not found"), "text/plain"))
    }
}