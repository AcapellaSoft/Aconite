package io.aconite.server.adapters

import io.aconite.server.CallAdapter
import io.aconite.utils.COROUTINE_SUSPENDED
import io.aconite.utils.cls
import java.util.concurrent.CompletableFuture
import javax.naming.OperationNotSupportedException
import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf

object CompletableFutureCallAdapter: CallAdapter {
    override fun adapt(fn: KFunction<*>): KFunction<*>? {
        val cls = fn.returnType.cls()
        if (!CompletableFuture::class.isSubclassOf(cls)) return null
        val type = fn.returnType.arguments[0].type!!

        return object<out R>: KFunction<R> {
            override val annotations = fn.annotations
            override val isAbstract = fn.isAbstract
            override val isExternal = fn.isExternal
            override val isFinal = fn.isFinal
            override val isInfix = fn.isInfix
            override val isInline = fn.isInline
            override val isOpen = fn.isOpen
            override val isOperator = fn.isOperator
            override val isSuspend = true
            override val name = fn.name
            override val parameters = fn.parameters
            override val returnType = type
            override val typeParameters = fn.typeParameters
            override val visibility = fn.visibility

            @Suppress("UNCHECKED_CAST")
            override fun call(vararg args: Any?): R {
                val c = args.last() as Continuation<R>
                val restArgs = args.take(args.size - 1)
                val future = fn.call(*restArgs.toTypedArray()) as CompletableFuture<R>
                future.whenComplete { res, ex ->
                    if (ex != null) c.resumeWithException(ex) else c.resume(res)
                }
                return COROUTINE_SUSPENDED as R
            }

            override fun callBy(args: Map<KParameter, Any?>): R {
                throw OperationNotSupportedException()
            }
        }
    }
}