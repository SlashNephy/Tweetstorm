package jp.nephy.tweetstorm.session

import io.ktor.features.origin
import io.ktor.request.ApplicationRequest
import jp.nephy.tweetstorm.builder.newStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.io.ByteWriteChannel

private val logger = jp.nephy.tweetstorm.logger("Tweetstorm.DemoStream")

class DemoStream(channel: ByteWriteChannel, request: ApplicationRequest): Stream<Unit>(channel, request) {
    override suspend fun await() {
        logger.info { "Unknown client: ${request.origin.remoteHost} has connected to DemoStream." }

        try {
            handler.emit(newStatus {
                text { "This is demo stream. Since Tweetstorm could not authenticate you, demo stream has started. Please check your config.json." }
            })

            while (handler.isAlive) {
                if (!handler.heartbeat()) {
                    break
                }
                delay(3000)
            }
        } finally {
            logger.info { "Unknown client: ${request.origin.remoteHost} has disconnected from DemoStream." }
        }
    }
}
