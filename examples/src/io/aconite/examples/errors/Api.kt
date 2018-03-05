package io.aconite.examples.errors

import io.aconite.annotations.GET
import io.aconite.annotations.POST

// You can catch and process exceptions in some stages in client/server pipeline.
// Without any processing, exceptions will be passed through aconite to outer exceptions handler (if any).
// There are some default implementations, that are subclasses of ErrorHandler, either on client or server.

interface ErrorsApi {
    @GET("/runtime")
    suspend fun runtimeError()

    @POST("/http")
    suspend fun httpError()
}