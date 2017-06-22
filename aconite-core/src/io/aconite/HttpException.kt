package io.aconite

open class HttpException(val code: Int, message: String? = null, cause: Throwable? = null): Exception(message, cause)

open class ArgumentMissingException(message: String? = null, cause: Throwable? = null): HttpException(400, message, cause)

open class MethodNotAllowedException(message: String? = null, cause: Throwable? = null): HttpException(405, message, cause)