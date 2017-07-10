package io.aconite.client

import io.aconite.BodySerializer
import io.aconite.Request
import io.aconite.Response
import io.aconite.StringSerializer
import io.aconite.serializers.BuildInStringSerializers
import io.aconite.serializers.SimpleBodySerializer

interface HttpClient {
    suspend fun makeRequest(request: Request): Response
}

class AconiteClient(
        val httpClient: HttpClient,
        val bodySerializer: BodySerializer.Factory = SimpleBodySerializer.Factory,
        val stringSerializer: StringSerializer.Factory = BuildInStringSerializers
)