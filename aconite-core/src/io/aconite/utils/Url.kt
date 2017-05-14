package io.aconite.utils

class UrlFormatException: Exception()

fun formatUrl(url: String) = '/' + url.trim('/')