package io.aconite.server

import io.aconite.BodySerializer
import io.aconite.Request
import io.aconite.Response
import io.aconite.StringSerializer
import io.aconite.serializers.BuildInStringSerializers
import io.aconite.serializers.SimpleBodySerializer
import io.aconite.server.adapters.SuspendCallAdapter
import io.aconite.server.filters.PassMethodFilter
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.createType

/**
 * Used to wrap functions, that are not `suspend`, but use some other form of an asynchronous call,
 * for example, functions, that return [CompletableFuture].
 */
interface CallAdapter {
    /**
     * Wrap [fn] to make it `suspend`. If this adapter does not support such type of functions,
     * then it must return `null`.
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
 * Main server class, that are used to register HTTP interfaces and accepts HTTP requests.
 *
 */
class AconiteServer(
        val bodySerializer: BodySerializer.Factory = SimpleBodySerializer.Factory,
        val stringSerializer: StringSerializer.Factory = BuildInStringSerializers,
        val callAdapter: CallAdapter = SuspendCallAdapter,
        val methodFilter: MethodFilter = PassMethodFilter,
        private val inner: RequestAcceptor = NotFoundRequestAcceptor
) : RequestAcceptor {
    companion object : RequestAcceptor.Factory<Configuration> {
        override fun create(inner: RequestAcceptor, configurator: Configuration.() -> Unit): RequestAcceptor {
            return Configuration().apply(configurator).build(inner)
        }
    }

    class Configuration {
        var bodySerializer: BodySerializer.Factory = SimpleBodySerializer.Factory
        var stringSerializer: StringSerializer.Factory = BuildInStringSerializers
        var callAdapter: CallAdapter = SuspendCallAdapter
        var methodFilter: MethodFilter = PassMethodFilter

        private val registrations = mutableListOf<(AconiteServer) -> Unit>()

        fun <T : Any> register(iface: KClass<T>, factory: () -> T) {
            registrations.add { it.register(iface, factory) }
        }

        inline fun <reified T : Any> register(noinline factory: () -> T) {
            register(T::class, factory)
        }

        fun <T : Any> register(obj: T, iface: KClass<T>) {
            register(iface) { obj }
        }

        fun build(inner: RequestAcceptor) = AconiteServer(
                bodySerializer, stringSerializer, callAdapter, methodFilter, inner
        ).apply {
            registrations.forEach { it(this) }
        }
    }

    private val modules = mutableListOf<RootHandler>()
    internal val interceptors = Interceptors(this)

    /**
     * Register factory [factory] of [iface] HTTP interface's implementations. All
     * functions wrapped by [callAdapter] and filtered by [methodFilter]. After this
     * call, handlers, that are represented as functions of the [iface] and all it
     * submodules, will be accessible through the HTTP requests to this server.
     */
    fun <T: Any> register(iface: KClass<T>, factory: () -> T) {
        modules.add(RootHandler(this, factory, iface.createType()))
    }

    /**
     * Shortcut with reified type [T].
     */
    inline fun <reified T: Any> register(noinline factory: () -> T) {
        register(T::class, factory)
    }

    /**
     * This function is like register(iface, factory), but with constant factory, that
     * always returns [obj].
     */
    fun <T: Any> register(obj: T, iface: KClass<T>) {
        register(iface) { obj }
    }

    /**
     * Accepts HTTP request and routes it to the corresponding handler. Serialization and
     * deserialization performed by the [bodySerializer] and [stringSerializer].
     * @param[url] url of requested handler
     * @param[request] HTTP request
     * @return HTTP response if handler was found, otherwise - `null`.
     */
    override suspend fun accept(url: String, request: Request): Response {
        for (router in modules)
            return router.accept(url, request) ?: continue
        return inner.accept(url, request)
    }
}