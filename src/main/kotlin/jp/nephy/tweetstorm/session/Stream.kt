package jp.nephy.tweetstorm.session

import io.ktor.http.Parameters
import jp.nephy.tweetstorm.escapeHtml
import jp.nephy.tweetstorm.escapeUnicode
import jp.nephy.tweetstorm.toBooleanEasy
import java.io.IOException
import java.io.Writer

abstract class Stream(private val writer: Writer, query: Parameters) {
    companion object {
        private const val delimiter = "\r\n"
    }

    private var isAliveBacking = false
    val isAlive: Boolean
        get() = isAliveBacking

    private val delimitedByLength = query["delimited"].equals("length", true)
    val stringifyFriendIds = query["stringify_friend_ids"].orEmpty().toBooleanEasy()

    abstract fun handle()

    fun start() {
        isAliveBacking = true
        handle()
    }

    @Synchronized
    private fun wrapWriter(content: String) {
        if (!isAlive){
            return
        }

        try {
            writer.write(content)
            writer.flush()
        } catch (e: IOException) {
            isAliveBacking = false
            throw e
        }
    }

    fun send(content: String) {
        val text = "${content.trim().escapeHtml().escapeUnicode()}$delimiter"
        if (delimitedByLength) {
            wrapWriter("${text.length}$delimiter$text")
        } else {
            wrapWriter(text)
        }
    }

    fun heartbeat() {
        wrapWriter(delimiter)
    }
}
