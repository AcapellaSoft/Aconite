package io.aconite.server

import io.aconite.utils.UrlTemplate

abstract class AbstractRouter: Comparable<AbstractRouter> {
    abstract val template: UrlTemplate
    abstract fun accept(obj: Any, url: String, request: Request): Response?
    final override fun compareTo(other: AbstractRouter) = template.compareTo(other.template)
}

class ModuleRouter(override val template: UrlTemplate, handlers: List<AbstractHandler>): AbstractRouter() {
    private val handlers = handlers.sorted().reversed()

    override fun accept(obj: Any, url: String, request: Request): Response? {
        val (rest, path) = template.parse(url) ?: return null
        val parsedRequest = request.copy(path = request.path + path)

        for (handler in handlers) {
            return handler.accept(obj, rest, parsedRequest) ?: continue
        }

        return null
    }
}