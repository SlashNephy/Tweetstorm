package jp.nephy.tweetstorm.session

import jp.nephy.tweetstorm.escapeHtml
import jp.nephy.tweetstorm.escapeUnicode
import java.io.IOException
import java.io.Writer

abstract class StreamWriter(private val writer: Writer): Stream {
    companion object {
        private const val delimiter = "\r\n"
    }

    private val delimitedBy by lazy {
        DelimitedBy.byName(request.queryParameters["delimited"].orEmpty())
    }

    private var isAliveBacking = true
    val isAlive: Boolean
        get() = isAliveBacking

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
        when (delimitedBy) {
            DelimitedBy.Length -> {
                wrapWriter("${text.length}$delimiter$text")
            }
            else -> {
                wrapWriter(text)
            }
        }
    }

    fun heartbeat() {
        wrapWriter(delimiter)
    }
}
