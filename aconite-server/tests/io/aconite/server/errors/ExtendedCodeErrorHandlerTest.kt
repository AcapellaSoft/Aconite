package io.aconite.server.errors

import com.google.gson.Gson
import io.aconite.ArgumentMissingException
import io.aconite.MethodNotAllowedException
import io.aconite.server.AconiteServerException
import org.junit.Assert
import org.junit.Test

class ExtendedCodeErrorHandlerTest {
    val gson = Gson()

    @Test(expected = AconiteServerException::class)
    fun testRegisterNotHttpException() {
        ExtendedCodeErrorHandler.register<NotHttpException>()
    }

    @Test(expected = AconiteServerException::class)
    fun testRegisterWithoutAnnotation() {
        ExtendedCodeErrorHandler.register<WithoutAnnotationException>()
    }

    @Test(expected = NotRegisteredException::class)
    fun testNotRegisteredException() {
        ExtendedCodeErrorHandler.handle(NotRegException())
    }

    @Test fun testNotHttpExceptionToResponse() {
        val response = ExtendedCodeErrorHandler.handle(RuntimeException())
        Assert.assertEquals(500, response.code)
        Assert.assertEquals("Internal server error", response.body?.content?.string)
    }

    @Test fun testArgumentMissingToResponse() {
        val response = ExtendedCodeErrorHandler.handle(ArgumentMissingException("arg1"))
        Assert.assertEquals(400, response.code)
        val message = gson.fromJson(response.body?.content?.string, ErrorResponseBody::class.java)
        Assert.assertEquals(0, message.code)
        Assert.assertEquals("arg1", message.message)
    }

    @Test fun testMethodNotAllowedToResponse() {
        val response = ExtendedCodeErrorHandler.handle(MethodNotAllowedException("method1"))
        Assert.assertEquals(405, response.code)
        val message = gson.fromJson(response.body?.content?.string, ErrorResponseBody::class.java)
        Assert.assertEquals(0, message.code)
        Assert.assertEquals("method1", message.message)
    }

    @Test fun testCustomExceptionToResponse() {
        ExtendedCodeErrorHandler.register<CustomHttpException>()
        val response = ExtendedCodeErrorHandler.handle(CustomHttpException())
        Assert.assertEquals(412, response.code)
        val message = gson.fromJson(response.body?.content?.string, ErrorResponseBody::class.java)
        Assert.assertEquals(1230, message.code)
        Assert.assertEquals("custom error", message.message)
    }
}