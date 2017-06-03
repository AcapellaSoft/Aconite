package io.aconite.server

import io.aconite.HttpError
import io.aconite.METHOD_NOT_ALLOWED
import io.aconite.utils.UrlTemplate

// TODO: remove superclass if no other subclasses will be added
abstract class AbstractRouter: Comparable<AbstractRouter> {
    abstract val template: UrlTemplate
    abstract suspend fun accept(obj: Any, url: String, request: Request): Pair<Response?, HttpError?>
    final override fun compareTo(other: AbstractRouter) = template.compareTo(other.template)
}

class ModuleRouter(override val template: UrlTemplate, handlers: List<AbstractHandler>): AbstractRouter() {
    private val handlers = handlers.sorted().reversed()

    override suspend fun accept(obj: Any, url: String, request: Request): Pair<Response?, HttpError?> {
        val (rest, path) = template.parse(url) ?: return Pair(null, null)
        val parsedRequest = request.copy(path = request.path + path)
        var resultError: HttpError? = null

        for (handler in handlers) {
            val (response, currentError) = handler.accept(obj, rest, parsedRequest)
            if (response != null) return Pair(response, null)
            resultError = currentError ?: resultError
        }

        return Pair(null, resultError ?: METHOD_NOT_ALLOWED)
    }
}