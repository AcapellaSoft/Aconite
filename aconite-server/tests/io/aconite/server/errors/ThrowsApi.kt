package io.aconite.server.errors

import io.aconite.HttpException
import io.aconite.annotations.Body
import io.aconite.annotations.GET

interface ThrowsApi {
    @GET suspend fun throwError(@Body body: String): String
}

class ThrowsImpl(val error: (String) -> Throwable): ThrowsApi {
    override suspend fun throwError(body: String) = throw error(body)
}

@HttpErrorCode(102)
class NotHttpException: Exception()

class WithoutAnnotationException: HttpException(400)

@HttpErrorCode(103)
class NotRegException: HttpException(400)

@HttpErrorCode(1230)
class CustomHttpException: HttpException(412, "custom error")