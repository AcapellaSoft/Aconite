package io.aconite

data class HttpError(val code: Int, val message: String)

val METHOD_NOT_ALLOWED = HttpError(405, "Method not allowed")
val RESOURCE_NOT_FOUND = HttpError(404, "Resource not found")