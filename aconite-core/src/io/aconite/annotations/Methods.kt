package io.aconite.annotations

/** Accepts custom [method] HTTP-method on [url] relative URL. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class HTTP(
        /** http-method name */
        val method: String,

        /** relative (in module) url */
        val url: String = ""
)

/**
 * The function marked by this annotation must return an interface
 * that represents the inner HTTP-module.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class MODULE(
        /** relative (in module) url */
        val value: String = ""
)

/** Accepts DELETE request on [value] relative URL. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class DELETE(
        /** relative (in module) url */
        val value: String = ""
)

/** Accepts GET request on [value] relative URL. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class GET(
        /** relative (in module) url */
        val value: String = ""
)

/** Accepts HEAD request on [value] relative URL. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class HEAD(
        /** relative (in module) url */
        val value: String = ""
)

/** Accepts OPTIONS request on [value] relative URL. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class OPTIONS(
        /** relative (in module) url */
        val value: String = ""
)

/** Accepts PATCH request on [value] relative URL. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class PATCH(
        /** relative (in module) url */
        val value: String = ""
)

/** Accepts POST request on [value] relative URL. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class POST(
        /** relative (in module) url */
        val value: String = ""
)

/** Accepts PUT request on [value] relative URL. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class PUT(
        /** relative (in module) url */
        val value: String = ""
)