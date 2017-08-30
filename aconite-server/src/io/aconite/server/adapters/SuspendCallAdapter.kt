package io.aconite.server.adapters

import io.aconite.server.CallAdapter
import io.aconite.utils.COROUTINE_SUSPENDED
import io.aconite.utils.asyncCall
import io.aconite.utils.toChannel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import javax.naming.OperationNotSupportedException
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.startCoroutine
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

object SuspendCallAdapter : CallAdapter {
    override fun adapt(fn: KFunction<*>): KFunction<*>? {
        if (!fn.isSuspend) return null

        val chanType = ReceiveChannel::class.createType(listOf(
                KTypeProjection.invariant(fn.returnType)
        ))

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
            override val returnType = chanType
            override val typeParameters = fn.typeParameters
            override val visibility = fn.visibility
            override fun toString() = fn.toString()

            @Suppress("UNCHECKED_CAST")
            override fun call(vararg args: Any?): R {
                val c = args.last() as Continuation<ReceiveChannel<R>>
                val restArgs = args.take(args.size - 1)

                val coroutine: suspend () -> ReceiveChannel<R> = {
                    val result = fn.asyncCall(*restArgs.toTypedArray()) as R
                    result.toChannel()
                }
                coroutine.startCoroutine(c)

                return COROUTINE_SUSPENDED as R
            }

            override fun callBy(args: Map<KParameter, Any?>): R {
                throw OperationNotSupportedException()
            }
        }
    }
}