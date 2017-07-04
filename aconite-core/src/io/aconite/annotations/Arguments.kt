package io.aconite.annotations

/**
 * The function argument marked by this annotation will contain
 * the body of the request. The value of the argument will be
 * serialized using the body serializer. If the request does
 * not contain this header then `null` will be passed as the
 * argument value.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Body

/**
 * The function argument marked by this annotation will contain
 * the [name] header of the request. If [name] is an empty string
 * then the argument name will be used. The argument value will
 * be serialized using the string serializer. If the request does
 * not contain this header then `null` will be passed as the
 * argument value.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Header(val name: String = "")

/**
 * The function argument marked by this annotation will contain
 * the [name] path parameter of the request. If [name] is an empty
 * string then the argument name will be used. The argument
 * value will be serialized using the string serializer. The
 * [name] path parameter must be presented in the request.
 * Path parameters are part of the url template captured by curly
 * brackets. For example: `/users/{userId}/first-name` url contains
 * `userId` path parameter.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Path(val name: String = "")

/**
 * The function argument marked by this annotation will contain
 * the [name] query parameter of the request. If [name] is an empty
 * string then the argument name will be used. The argument value
 * will be serialized using the string serializer. If the request
 * does not contain this query then `null` will be passed as the
 * argument value.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Query(val name: String = "")