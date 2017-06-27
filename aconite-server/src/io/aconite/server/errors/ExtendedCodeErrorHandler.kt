package io.aconite.server.errors

import com.google.gson.Gson
import io.aconite.*
import io.aconite.server.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class HttpErrorCode(val extCode: Int)

class NotRegisteredException(message: String): RuntimeException(message)

internal data class ErrorResponseBody(val code: Int, val message: String)

object ExtendedCodeErrorHandler: ErrorHandler {
    private val ex2error = ConcurrentHashMap<KClass<*>, Int>()
    private val gson = Gson()
    private val logger = LoggerFactory.getLogger(ExtendedCodeErrorHandler::class.java)

    init {
        // 400
        ex2error[BadRequestException::class] = 0
        ex2error[ArgumentMissingException::class] = 1
        // 405
        ex2error[MethodNotAllowedException::class] = 0
        // 415
        ex2error[UnsupportedMediaTypeException::class] = 0
    }

    fun <T: Any> register(cls: KClass<T>) {
        if (!cls.isSubclassOf(HttpException::class))
            throw AconiteServerException("Trying to register exception '$cls' but only subclasses of HttpException is allowed")

        val annotation = cls.findAnnotation<HttpErrorCode>()
                ?: throw AconiteServerException("HttpErrorCode annotation not found in class '$cls'")

        ex2error[cls] = annotation.extCode
    }

    inline fun <reified T: Any> register() = register(T::class)

    override fun handle(ex: Throwable): Response {
        return when (ex) {
            is HttpException -> {
                val cls = ex::class
                val extCode = ex2error[cls] ?: throw NotRegisteredException("Exception of class '$cls' is not registered")
                val body = ErrorResponseBody(extCode, ex.message ?: "")
                Response(code = ex.code, body = BodyBuffer(
                        content = Buffer.wrap(gson.toJson(body, ErrorResponseBody::class.java)),
                        contentType = "application/json"
                ))
            }
            else -> {
                logger.error("Internal server error", ex)
                Response(code = 500, body = BodyBuffer(Buffer.wrap("Internal server error"), "plain/text"))
            }
        }
    }
}