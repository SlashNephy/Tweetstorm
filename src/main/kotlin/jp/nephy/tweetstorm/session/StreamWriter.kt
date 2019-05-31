package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest
import jp.nephy.jsonkt.JsonObject
import jp.nephy.jsonkt.delegation.JsonModel
import jp.nephy.jsonkt.jsonObjectOf
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.endpoints.stream.StreamDelimitedBy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.io.writeStringUtf8
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.io.IOException

class StreamWriter(private val channel: ByteWriteChannel, request: ApplicationRequest) {
    companion object {
        private const val delimiter = "\r\n"
        private val logger = jp.nephy.tweetstorm.logger("Tweetstorm.StreamContent")
    }

    private val delimitedBy = if (request.queryParameters["delimited"] == "length") StreamDelimitedBy.Length else StreamDelimitedBy.Default
    private val lock = Mutex()

    val isAlive: Boolean
        get() = !channel.isClosedForWrite

    private suspend fun writeWrap(content: String): Boolean {
        if (!isAlive){
            return false
        }

        return try {
            lock.withLock {
                withTimeout(1000) {
                    channel.writeStringUtf8(content)
                    channel.flush()
                }
            }
            true
        } catch (e: CancellationException) {
            false
        } catch (e: IOException) {
            false
        }
    }

    private suspend fun emit(content: String): Boolean {
        logger.trace { "Payload = $content" }

        val text = "${content.trim().escapeHtml().escapeUnicode()}$delimiter"
        return when (delimitedBy) {
            StreamDelimitedBy.Length -> {
                writeWrap("${text.length}$delimiter$text")
            }
            else -> {
                writeWrap(text)
            }
        }
    }

    suspend fun emit(vararg pairs: Pair<String, Any?>): Boolean {
        return emit(jsonObjectOf(*pairs))
    }

    suspend fun emit(json: JsonObject): Boolean {
        return emit(json.toJsonString())
    }

    suspend fun emit(payload: JsonModel): Boolean {
        return emit(payload.json)
    }

    suspend fun heartbeat(): Boolean {
        return writeWrap(delimiter)
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
}