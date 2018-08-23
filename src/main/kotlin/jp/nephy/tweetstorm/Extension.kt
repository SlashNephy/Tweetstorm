package jp.nephy.tweetstorm

import java.net.URLEncoder

internal fun String.escapeHtml(): String {
    return replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

internal fun String.escapeUnicode(): String {
    return map {
        val code = it.toInt()
        if (code in 0 until 128) {
            "$it"
        } else {
            String.format("\\u%04x", code)
        }
    }.joinToString("")
}

internal fun String.encodeURL(): String {
    return URLEncoder.encode(this, "UTF-8").replace("%7E", "~").map {
        when (it) {
            '+' -> "%20"
            '*' -> "%2A"
            else -> "$it"
        }
    }.joinToString("")
}

internal fun String.toBooleanEasy(): Boolean {
    return equals("true", true) || equals("1")
}
