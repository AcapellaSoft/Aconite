package io.aconite.utils

private val URL_PARAM_REGEX = Regex("^\\{(?<param>[^/{}]+)}|^(?<text>[^{}]+)")
private val URL_VALIDATION_REGEX = Regex("^(?:\\{[^/{}]+}|[^{}]+)+$")

/**
 * Raises when string passed to [UrlTemplate] is not an url.
 * */
class UrlFormatException(message: String): Exception(message)

/**
 * Converts a [url] to string with one leading slash and without
 * trailing slash.
 * @return converted url string
 */
fun formatUrl(url: String) = '/' + url.trim('/')

/**
 * Class for converting the url-template string to regex for routing.
 * The url can contain parameters (in curly brackets), witch will be
 * converted to the capturing groups. This class supports comparing
 * to order the urls for right routing. For example, this url-templates:
 * ```
 * /baz
 * /baz/{id}
 * /foo/bar
 * /baz/bar
 * ```
 * will be placed in this order:
 * ```
 * /baz/bar
 * /baz/{id}
 * /baz
 * /foo/bar
 * ```
 * so, url `/baz/bar` will be accepted by first template, although it
 * can be accepted by the second template too.
 * @param[url] url-template string
 */
class UrlTemplate(url: String): Comparable<UrlTemplate> {
    private val parts: List<UrlPart>
    private val params: List<String>
    private val regex: Regex

    init {
        val formattedUrl = formatUrl(url)
        validateUrl(formattedUrl)
        val (parts, params) = buildParts(formattedUrl)
        this.parts = parts
        this.params = params
        this.regex = Regex("^" + parts.map { it.toRegex() }.joinToString(""))
    }

    /**
     * Trying to match the beginning of the [url] to the url-template regex.
     * If it matches, returns the rest of the [url] and captured path parameters,
     * else - returns `null`.
     * @return pair: the rest of the [url] and captured path parameters
     */
    fun parse(url: String): Pair<String, Map<String, String>>? {
        val match = regex.find(url) ?: return null
        return parseInner(match, url)
    }

    /**
     * Trying to match the entire [url] to the url-template regex. If it matches,
     * returns captured path parameters, else - returns `null`.
     * @return captured path parameters
     */
    fun parseEntire(url: String): Map<String, String>? {
        val match = regex.matchEntire(url) ?: return null
        return parseInner(match, url).second
    }

    private fun parseInner(match: MatchResult, url: String): Pair<String, Map<String, String>> {
        val values = params
                .mapIndexed { idx, name -> Pair(name, match.groups[idx + 1]?.value) }
                .mapNotNull { it.allOrNull() }
                .toMap()
        val parsedUrl = formatUrl(url.drop(match.range.last + 1))
        return Pair(parsedUrl, values)
    }

    private fun validateUrl(url: String) {
        if (!URL_VALIDATION_REGEX.matches(url))
            throw UrlFormatException("String '$url' is not url")
    }

    private fun buildParts(url: String): Pair<List<UrlPart>, List<String>> {
        var s = url
        val parts = mutableListOf<UrlPart>()
        val params = mutableListOf<String>()

        while (s != "") {
            val part = URL_PARAM_REGEX.find(s)!! // checked to be not null by validation
            val gParam = part.groups["param"]
            val gText = part.groups["text"]

            assert(gParam != null || gText != null)
            if (gParam != null) {
                parts.add(ParameterUrlPart())
                params.add(gParam.value)
            } else if (gText != null) {
                parts.add(TextUrlPart(gText.value))
            }

            s = s.drop(part.range.last + 1)
        }

        return Pair(parts, params)
    }

    private fun getPart(index: Int) = if (index < parts.size) this.parts[index] else EmptyUrlPart

    override fun compareTo(other: UrlTemplate): Int {
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

private interface UrlPart: Comparable<UrlPart> {
    fun toRegex(): String
}

private class TextUrlPart(val text: String): UrlPart {
    override fun compareTo(other: UrlPart): Int {
        return when (other) {
            is TextUrlPart -> this.text.compareTo(other.text)
            is ParameterUrlPart -> 1
            is EmptyUrlPart -> 1
            else -> throw NotImplementedError()
        }
    }

    override fun toRegex() = Regex.escape(text)
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

    override fun toRegex() = "([^/]+)"
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

    override fun toRegex() = ""
}