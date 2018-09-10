package jp.nephy.tweetstorm.session

import io.ktor.features.origin
import io.ktor.request.ApplicationRequest
import jp.nephy.tweetstorm.builder.newStatus
import kotlinx.coroutines.experimental.delay
import java.io.IOException
import java.io.Writer
import java.util.concurrent.TimeUnit

private val logger = jp.nephy.tweetstorm.logger("Tweetstorm.DemoStream")

class DemoStream(writer: Writer, request: ApplicationRequest): Stream<Unit>(writer, request) {
    override suspend fun await() {
        logger.info { "Unknown client: ${request.origin.remoteHost} has connected to DemoStream." }

        try {
            handler.emit(newStatus {
                text { "This is demo stream. Since Tweetstorm could not authenticate you, demo stream has started. Please check your config.json." }
            })

            while (handler.isAlive) {
                handler.heartbeat()
                delay(10, TimeUnit.SECONDS)
            }
        } catch (e: IOException) {
        } finally {
            logger.info { "Unknown client: ${request.origin.remoteHost} has disconnected from DemoStream." }
        }
    }
}
