package io.aconite.server

import io.aconite.utils.UrlFormatException

private val URL_PARAM_REGEX = Regex("^(?<param>\\{([^/{}]+)})|^(?<text>[^{}]+)")

class ComparableUrl(url: String): Comparable<ComparableUrl> {
    private val parts = buildParts(url)

    private fun buildParts(url: String): List<UrlPart> {
        var s = url
        val result = mutableListOf<UrlPart>()

        while (s != "") {
            val part = URL_PARAM_REGEX.find(s) ?: throw UrlFormatException()
            val gParam = part.groups["param"]
            val gText = part.groups["text"]

            assert(gParam != null || gText != null)
            result.add(if (gText != null) TextUrlPart(gText.value) else ParameterUrlPart())

            s = s.drop(part.range.last + 1)
        }

        return result
    }

    private fun getPart(index: Int) = if (index < parts.size) this.parts[index] else EmptyUrlPart

    override fun compareTo(other: ComparableUrl): Int {
        val size = maxOf(this.parts.size, other.parts.size)
        var i = 0
        var r = 0

        while (i < size && r == 0) {
            val a = this.getPart(i)
            val b = other.getPart(i)
            r = a.compareTo(b)
            ++i
        }

        return r
    }
}

private interface UrlPart: Comparable<UrlPart>

private class TextUrlPart(val text: String): UrlPart {
    override fun compareTo(other: UrlPart): Int {
         return when (other) {
             is TextUrlPart -> this.text.compareTo(other.text)
             is ParameterUrlPart -> 1
             is EmptyUrlPart -> 1
             else -> throw NotImplementedError()
         }
    }
}

private class ParameterUrlPart: UrlPart {
    override fun compareTo(other: UrlPart): Int {
        return when (other) {
            is TextUrlPart -> -1
            is ParameterUrlPart -> 0
            is EmptyUrlPart -> 1
            else -> throw NotImplementedError()
        }
    }
}

private object EmptyUrlPart: UrlPart {
    override fun compareTo(other: UrlPart): Int {
        return when (other) {
            is TextUrlPart -> -1
            is ParameterUrlPart -> -1
            is EmptyUrlPart -> 0
            else -> throw NotImplementedError()
        }
    }
}