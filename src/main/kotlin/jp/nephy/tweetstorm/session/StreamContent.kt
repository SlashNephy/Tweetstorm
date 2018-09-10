package jp.nephy.tweetstorm.session

import com.google.gson.JsonObject
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.request.ApplicationRequest
import io.ktor.util.cio.writer
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.jsonObject
import jp.nephy.jsonkt.toJsonString
import jp.nephy.tweetstorm.escapeHtml
import jp.nephy.tweetstorm.escapeUnicode
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import java.io.IOException
import java.io.Writer

private const val delimiter = "\r\n"

class StreamContent(private val writer: suspend (writer: Writer) -> Unit): OutgoingContent.WriteChannelContent() {
    override val status = HttpStatusCode.OK
    override val headers = headersOf(HttpHeaders.Connection, "keep-alive")
    override val contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8)

    override suspend fun writeTo(channel: ByteWriteChannel) {
        channel.writer(Charsets.UTF_8).use {
            try {
                writer(it)
            } catch (e: IOException) {
                return
            }
        }
    }

    class Handler(private val writer: Writer, request: ApplicationRequest) {
        private val delimitedBy = DelimitedBy.byName(request.queryParameters["delimited"].orEmpty())
        private val lock = Mutex()

        var isAlive = true
            private set

        private suspend fun writeWrap(content: String) {
            if (!isAlive){
                return
            }

            lock.withLock {
                try {
                    writer.write(content)
                    writer.flush()
                } catch (e: IOException) {
                    isAlive = false
                    throw e
                }
            }
        }

        suspend fun emit(content: String) {
            val text = "${content.trim().escapeHtml().escapeUnicode()}$delimiter"
            when (delimitedBy) {
                DelimitedBy.Length -> {
                    writeWrap("${text.length}$delimiter$text")
                }
                else -> {
                    writeWrap(text)
                }
            }
        }

        suspend fun emit(vararg pairs: Pair<String, Any?>) {
            emit(jsonObject(*pairs))
        }

        suspend fun emit(json: JsonObject) {
            emit(json.toJsonString())
        }

        suspend fun emit(payload: JsonModel) {
            emit(payload.json)
        }

        suspend fun heartbeat() {
            writeWrap(delimiter)
        }
    }
}
