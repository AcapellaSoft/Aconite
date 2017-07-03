package io.aconite.server.complextest

import com.google.gson.Gson
import io.aconite.server.*
import io.aconite.server.errors.LogErrorHandler
import io.aconite.server.serializers.GsonBodySerializer
import org.junit.Assert
import org.junit.Test

class Test {
    val gson = Gson()

    @Test fun test_user_commandQueue_getAll() = asyncTest {
        val server = AconiteServer(
                bodySerializer = GsonBodySerializer.Factory(gson),
                errorHandler = LogErrorHandler
        )
        val root = RootImpl()
        server.register(root, RootApi::class)

        val response = server.accept("/users/$USER_1/command-queues/seq-1", Request(
                method = "GET"
        ))
        val expected = gson.toJson(root.commandQueues[USER_1]!!["seq-1"]!!)
        Assert.assertEquals(expected, response.body())
    }

    @Test fun test_user_commandQueue_get() = asyncTest {
        val server = AconiteServer(
                bodySerializer = GsonBodySerializer.Factory(gson),
                errorHandler = LogErrorHandler
        )
        val root = RootImpl()
        server.register(root, RootApi::class)

        val response = server.accept("/users/$USER_1/command-queues/seq-1/1", Request(
                method = "GET"
        ))
        val expected = gson.toJson(root.commandQueues[USER_1]!!["seq-1"]!![1L]!!)
        Assert.assertEquals(expected, response.body())
    }

    @Test fun test_user_commandQueue_put() = asyncTest {
        val server = AconiteServer(
                bodySerializer = GsonBodySerializer.Factory(gson),
                errorHandler = LogErrorHandler
        )
        val root = RootImpl()
        server.register(root, RootApi::class)

        val expected = Command("new-command", "1234")
        val response = server.accept("/users/$USER_1/command-queues/seq-1/345", Request(
                method = "PUT",
                body = BodyBuffer(
                        content = Buffer.wrap(gson.toJson(expected)),
                        contentType = "application/json"
                )
        ))
        Assert.assertEquals(200, response?.code)
        Assert.assertEquals(expected, root.commandQueues[USER_1]!!["seq-1"]!![345L])
    }
}