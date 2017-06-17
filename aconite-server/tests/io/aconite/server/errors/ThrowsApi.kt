package io.aconite.server.errors

import io.aconite.annotations.Body
import io.aconite.annotations.GET
import io.aconite.server.AconiteServer

interface ThrowsApi {
    @GET suspend fun throwError(@Body body: String): String
}

class ThrowsImpl: ThrowsApi {
    override suspend fun throwError(body: String) = throw RuntimeException(body)
}