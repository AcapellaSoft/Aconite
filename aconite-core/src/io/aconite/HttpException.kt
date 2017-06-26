package io.aconite

abstract class HttpException(val code: Int, message: String? = null, cause: Throwable? = null): Exception(message, cause)

open class BadRequestException(message: String? = null, cause: Throwable? = null): HttpException(400, message, cause)

open class ArgumentMissingException(message: String? = null, cause: Throwable? = null): BadRequestException(message, cause)

open class MethodNotAllowedException(message: String? = null, cause: Throwable? = null): HttpException(405, message, cause)

open class UnsupportedMediaTypeException(message: String? = null, cause: Throwable? = null): HttpException(415, message, cause)