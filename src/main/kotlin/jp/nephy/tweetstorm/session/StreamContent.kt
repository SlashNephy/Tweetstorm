package jp.nephy.tweetstorm.session

import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.io.ByteWriteChannel

class StreamContent(private val block: suspend (channel: ByteWriteChannel) -> Unit): OutgoingContent.WriteChannelContent() {
    override val status = HttpStatusCode.OK
    override val headers = headersOf(HttpHeaders.Connection, "keep-alive")
    override val contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8)

    override suspend fun writeTo(channel: ByteWriteChannel) {
        block(channel)
    }
}
