package dev.mokksy.mokksy.request

internal enum class MatcherCategory {
    METHOD,
    PATH,
    HEADERS,
    BODY,
    BODY_STRING {
        override fun toString() = "bodyString"
    },
    COOKIES,
    QUERY_PARAMS,
    FORM,
    MULTIPART,
    BYTES;

    override fun toString(): String = name.lowercase()
}
