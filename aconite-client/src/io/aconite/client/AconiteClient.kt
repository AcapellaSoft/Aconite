package io.aconite.client

import io.aconite.Request
import io.aconite.Response

interface HttpClient {
    suspend fun makeRequest(request: Request): Response
}

class AconiteClient(
        val httpClient: HttpClient
)