package jp.nephy.tweetstorm.session

import com.google.gson.JsonObject
import io.ktor.http.Parameters
import jp.nephy.jsonkt.JsonKt
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.jsonObject
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

    private fun send(content: String) {
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

    fun send(vararg pairs: Pair<String, Any?>) {
        send(jsonObject(*pairs))
    }

    fun send(json: JsonObject) {
        send(JsonKt.toJsonString(json))
    }

    fun send(payload: JsonModel) {
        send(payload.json)
    }

    fun heartbeat() {
        lock.withLock {
            writer.write("\r\n")
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
