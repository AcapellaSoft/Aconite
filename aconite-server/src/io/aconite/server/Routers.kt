package io.aconite.server

import io.aconite.utils.UrlTemplate

// TODO: remove superclass if no other subclasses will be added
abstract class AbstractRouter: Comparable<AbstractRouter> {
    abstract val template: UrlTemplate
    abstract suspend fun accept(obj: Any, url: String, request: Request): Response?
    final override fun compareTo(other: AbstractRouter) = template.compareTo(other.template)
}

class ModuleRouter(override val template: UrlTemplate, handlers: List<AbstractHandler>): AbstractRouter() {
    private val handlers = handlers.sorted().reversed()

    override suspend fun accept(obj: Any, url: String, request: Request): Response? {
        val (rest, path) = template.parse(url) ?: return null
        val parsedRequest = request.copy(path = request.path + path)

        for (handler in handlers) {
            return handler.accept(obj, rest, parsedRequest) ?: continue
        }

        return null
    }
}