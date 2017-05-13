package io.aconite.server

import io.aconite.Request
import io.aconite.utils.allOrNull
import io.aconite.utils.formatUrl

private val URL_PARAM_REGEX = Regex("\\{([^/{}]+)}")

interface RequestFilter {
    fun apply(request: Request): Request?
}

class RequestMatcher(private val filters: List<RequestFilter>) {

    class Builder {
        private val filters = mutableListOf<RequestFilter>()

        private fun add(fn: () -> RequestFilter) = this.apply { filters.add(fn()) }
        fun path(url: String, full: Boolean) = add { PathFilter(url, full) }

        fun build(): RequestMatcher = RequestMatcher(filters)
    }

    private fun apply(request: Request?, filter: RequestFilter): Request? {
        if (request == null) return null
        return filter.apply(request)
    }

    fun match(request: Request): Request? {
        return filters.fold(request, this::apply)
    }
}

private class PathFilter(url: String, full: Boolean): RequestFilter {
    private val args: List<String>
    private val regex: Regex

    init {
        val formattedUrl = formatUrl(url)
        var regexUrlStr = '^' + formattedUrl.replace(URL_PARAM_REGEX, "([^/]+)")
        if (full) regexUrlStr += '$'

        args = URL_PARAM_REGEX.findAll(formattedUrl)
                .mapNotNull { it.groups[1]?.value }
                .toList()
        regex = regexUrlStr.toRegex()
    }

    override fun apply(request: Request): Request? {
        val match = regex.find(request.url) ?: return null
        val values = args
                .mapIndexed { idx, name -> Pair(name, match.groups[idx + 1]?.value) }
                .mapNotNull { it.allOrNull() }
                .toMap()
        return request.copy(
                path = request.path + values,
                url = request.url.drop(match.range.last + 1)
        )
    }
}