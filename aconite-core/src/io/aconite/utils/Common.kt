package io.aconite.utils

/**
 * Extension for converting the pair with at least one null value
 * into the null pair.
 * @receiver not-null pair with nullable values
 * @return nullable pair with not-null values
 */
fun <A: Any, B: Any> Pair<A?, B?>.allOrNull(): Pair<A, B>? {
    val first = this.first
    val second = this.second
    if (first == null || second == null) return null
    return Pair(first, second)
}