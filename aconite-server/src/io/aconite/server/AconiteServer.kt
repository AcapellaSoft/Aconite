package io.aconite.server

import io.aconite.*
import io.aconite.utils.toChannel
import io.aconite.server.adapters.SuspendCallAdapter
import io.aconite.server.errors.PassErrorHandler
import io.aconite.server.filters.PassMethodFilter
import io.aconite.serializers.BuildInStringSerializers
import io.aconite.serializers.SimpleBodySerializer
import kotlinx.coroutines.experimental.Unconfined
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Used to wrap functions, that are not `suspend`, but use some other form of an asynchronous call,
 * for example, functions, that return [CompletableFuture].
 */
interface CallAdapter {
    /**
     * Wrap [fn] to make it `suspend`. If this adapter does not support such type of functions,
     * then it must return `null`. Return value of the function must be [ReceiveChannel] for
     * streaming support. If it returns only one value, then [toChannel] can be used.
     * @param[fn] function to wrap
     * @return wrapped function or `null` if not supported
     */
    fun adapt(fn: KFunction<*>): KFunction<*>?
}

/**
 * Used to specify, which functions would be registered as HTTP handlers. It may be useful, for example,
 * if you need to split your server into many threads, and first thread need to serve one part of handlers,
 * when second thread - another part of handlers.
 */
interface MethodFilter {
    /**
     * @return true if [fn] must be registered, otherwise - false
     */
    fun predicate(fn: KFunction<*>): Boolean
}

/**
 * Used to convert exception, that was thrown from handler, into error HTTP response.
 * Simplest example is to convert all exceptions into `500 Internal Server Error`.
 */
interface ErrorHandler {
    /**
     * Converts [ex] to error [Response].
     */
    fun handle(ex: Throwable): Response
}

/**
 * Main server class, that are used to register HTTP interfaces and accepts HTTP requests.
 *
 */
class AconiteServer(
        val bodySerializer: BodySerializer.Factory = SimpleBodySerializer.Factory,
        val stringSerializer: StringSerializer.Factory = BuildInStringSerializers,
        val callAdapter: CallAdapter = SuspendCallAdapter,
        val methodFilter: MethodFilter = PassMethodFilter,
        val errorHandler: ErrorHandler = PassErrorHandler,
        val coroutineContext: CoroutineContext = Unconfined
) {
    private val modules = mutableListOf<RootHandler>()

    /**
     * Register [obj] implementation of [iface] HTTP interface. All functions wrapped by
     * [callAdapter] and filtered by [methodFilter]. After this call, handlers, that are
     * represented as functions of the [iface] and all it submodules, will be accessible
     * through the HTTP requests to this server.
     */
    fun <T: Any> register(obj: T, iface: KClass<T>) {
        modules.add(RootHandler(this, obj, iface.createType()))
    }

    /**
     * Accepts HTTP request and routes it to the corresponding handler. Serialization and
     * deserialization performed by the [bodySerializer] and [stringSerializer]. If handler
     * fails with exception, function will return error response, that are built by [errorHandler].
     * @param[url] url of requested handler
     * @param[request] HTTP request
     * @return HTTP response if handler was found, otherwise - `null`.
     */
    suspend fun accept(url: String, request: Request): Response? {
        try {
            for (router in modules)
                return router.accept(url, request) ?: continue
            return null
        } catch (ex: Throwable) {
            return errorHandler.handle(ex)
        }
    }
}