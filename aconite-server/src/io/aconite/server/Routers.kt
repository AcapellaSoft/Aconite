package io.aconite.server

import io.aconite.*
import io.aconite.utils.UrlTemplate

class Router(val template: UrlTemplate, handlers: List<AbstractHandler>): Comparable<Router> {
    private val handlers = handlers.sorted().reversed()

    suspend fun accept(obj: Any, url: String, request: Request): Response? {
        val (rest, path) = template.parse(url) ?: return null
        val parsedRequest = request.copy(path = request.path + path)
        var error: BadRequestException? = null

        for (handler in handlers) try {
            return handler.accept(obj, rest, parsedRequest) ?: continue
        } catch (ex: BadRequestException) {
            error = ex
        }

        if (error != null) throw error
        throw MethodNotAllowedException("Method not allowed")
    }

    override fun compareTo(other: Router) = template.compareTo(other.template)
}