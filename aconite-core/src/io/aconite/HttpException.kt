package io.aconite

abstract class HttpException(val code: Int, message: String? = null, cause: Throwable? = null): Exception(message, cause)

class ArgumentMissingException(message: String? = null, cause: Throwable? = null): HttpException(400, message, cause)

class MethodNotAllowedException(message: String? = null, cause: Throwable? = null): HttpException(405, message, cause)