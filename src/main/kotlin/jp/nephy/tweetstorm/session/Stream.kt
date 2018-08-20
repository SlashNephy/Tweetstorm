package jp.nephy.tweetstorm.session

import com.google.gson.JsonObject
import io.ktor.http.Parameters
import jp.nephy.jsonkt.JsonKt
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.jsonObject
import java.io.Writer

abstract class Stream(private val writer: Writer, query: Parameters) {
    companion object {
        private const val delimiter = "\r\n"
    }

    private val delimitedByLength = query["delimited"] == "length"
    val stringifyFriendIds = query["stringify_friend_ids"].orEmpty().toBoolean()
    val repliesAll = query["replies"] == "all"

    abstract fun handle()

    private fun send(content: String) {
        val text = "${content.trim().escapeHtml().escapeUnicode()}$delimiter"
        if (delimitedByLength) {
            writer.write("${text.length}$delimiter")
            writer.flush()
        }
        writer.write(text)
        writer.flush()
    }

    fun send(vararg pairs: Pair<String, Any?>) {
        send(jsonObject(*pairs))
    }

    fun send(json: JsonObject) {
        send(JsonKt.toJsonString(json))
    }

    fun send(model: JsonModel) {
        send(model.json)
    }

    fun heartbeat() {
        writer.write("\r\n")
        writer.flush()
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
