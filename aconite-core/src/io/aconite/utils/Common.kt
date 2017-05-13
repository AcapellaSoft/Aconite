package io.aconite.utils

fun <A: Any, B: Any> Pair<A?, B?>.allOrNull(): Pair<A, B>? {
    val first = this.first
    val second = this.second
    if (first == null || second == null) return null
    return Pair(first, second)
}