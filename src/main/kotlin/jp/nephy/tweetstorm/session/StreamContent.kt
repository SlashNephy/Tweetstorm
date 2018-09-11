package jp.nephy.tweetstorm.session

import com.google.gson.JsonObject
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.request.ApplicationRequest
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.jsonObject
import jp.nephy.jsonkt.toJsonString
import jp.nephy.tweetstorm.escapeHtml
import jp.nephy.tweetstorm.escapeUnicode
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.coroutines.experimental.io.writeStringUtf8
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import kotlinx.coroutines.experimental.withTimeout
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val delimiter = "\r\n"
private val logger = jp.nephy.tweetstorm.logger("Tweetstorm.StreamContent")

class StreamContent(private val writer: suspend (channel: ByteWriteChannel) -> Unit): OutgoingContent.WriteChannelContent() {
    override val status = HttpStatusCode.OK
    override val headers = headersOf(HttpHeaders.Connection, "keep-alive")
    override val contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8)

    override suspend fun writeTo(channel: ByteWriteChannel) {
        runBlocking {
            writer(channel)
        }
    }

    class Handler(private val channel: ByteWriteChannel, request: ApplicationRequest) {
        private val delimitedBy = DelimitedBy.byName(request.queryParameters["delimited"].orEmpty())
        private val lock = Mutex()

        val isAlive: Boolean
            get() = !channel.isClosedForWrite

        private suspend fun writeWrap(content: String): Boolean {
            if (channel.isClosedForWrite){
                return false
            }

            return try {
                lock.withLock {
                    withTimeout(1, TimeUnit.SECONDS) {
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
                DelimitedBy.Length -> {
                    writeWrap("${text.length}$delimiter$text")
                }
                else -> {
                    writeWrap(text)
                }
            }
        }

        suspend fun emit(vararg pairs: Pair<String, Any?>): Boolean {
            return emit(jsonObject(*pairs))
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
    }
}
