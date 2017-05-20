package io.aconite.server

abstract class AbstractHandler : Comparable<AbstractHandler> {
    abstract val argsCount: Int
    abstract fun accept(url: String, request: Request): Response?
    final override fun compareTo(other: AbstractHandler) = argsCount.compareTo(other.argsCount)
}