package jp.nephy.tweetstorm.web.routing

import io.ktor.application.ApplicationCall
import io.ktor.response.respond
import jp.nephy.tweetstorm.logger
import jp.nephy.tweetstorm.session.StreamContent
import kotlinx.coroutines.io.ByteWriteChannel

internal val logger = logger("Tweetstorm.Routing")

suspend fun ApplicationCall.respondStream(writer: suspend (channel: ByteWriteChannel) -> Unit) {
    respond(StreamContent(writer))
}
