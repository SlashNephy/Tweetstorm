package jp.nephy.tweetstorm.session

import io.ktor.http.Parameters
import java.io.Writer
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class Stream(private val writer: Writer, query: Parameters) {
    companion object {
        private const val delimiter = "\r\n"
    }

    private val lock = ReentrantLock()
    private val delimitedByLength = query["delimited"] == "length"
    val stringifyFriendIds = query["stringify_friend_ids"].orEmpty().toBoolean()

    abstract fun handle()

    fun send(content: String) {
        val text = "${content.trim().escapeHtml().escapeUnicode()}$delimiter"
        lock.withLock {
            if (delimitedByLength) {
                writer.write("${text.length}$delimiter$text")
                writer.flush()
            } else {
                writer.write(text)
                writer.flush()
            }
        }
    }

    fun heartbeat() {
        lock.withLock {
            writer.write(delimiter)
            writer.flush()
        }
    }
}

private fun String.escapeHtml(): String {
    return replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

private fun String.escapeUnicode(): String {
    return map {
        val code = it.toInt()
        if (code in 0 until 128) {
            "$it"
        } else {
            String.format("\\u%04x", code)
        }
    }.joinToString("")
}
