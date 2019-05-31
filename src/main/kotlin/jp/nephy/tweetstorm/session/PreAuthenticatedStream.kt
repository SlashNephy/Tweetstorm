package jp.nephy.tweetstorm.session

import io.ktor.features.origin
import io.ktor.request.ApplicationRequest
import io.ktor.request.host
import jp.nephy.penicillin.extensions.models.builder.newStatus
import jp.nephy.tweetstorm.Config
import kotlinx.coroutines.delay
import kotlinx.coroutines.io.ByteWriteChannel
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

private val logger = jp.nephy.tweetstorm.logger("Tweetstorm.PreAuthenticatedStream")

class PreAuthenticatedStream(channel: ByteWriteChannel, request: ApplicationRequest, val account: Config.Account): Stream(channel, request) {
    companion object {
        private val streams = CopyOnWriteArrayList<PreAuthenticatedStream>()

        @Synchronized
        fun auth(urlToken: String, accountToken: String): Boolean {
            return streams.removeIf { it.urlToken == urlToken && it.account.token == accountToken }
        }

        @Synchronized
        fun check(urlToken: String): Boolean {
            return streams.count { it.urlToken == urlToken } > 0
        }

        @Synchronized
        private fun contain(stream: PreAuthenticatedStream): Boolean {
            return streams.count { it.urlToken == stream.urlToken && it.account.token == stream.account.token } > 0
        }

        @Synchronized
        private fun register(stream: PreAuthenticatedStream) {
            streams += stream
        }
    }

    private val urlToken = UUID.randomUUID().toString().toLowerCase().replace("-", "")

    override suspend fun await(): Boolean {
        logger.info { "Client: @${account.user.screenName} (${request.origin.remoteHost}) requested account-token authentication." }

        register(this)

        writer.emit(newStatus {
            user {
                name("Tweetstorm Authenticator")
            }
            text { "To start streaming, access https://${request.host()}/auth/token/$urlToken" }
            url("https://${request.host()}/auth/token/$urlToken", 27, 101)
        })

        repeat(300) {
            if (!contain(this)) {
                return true
            }
            else if (it % 10 == 0 && !writer.heartbeat()) {
                return false
            }

            delay(1000)
        }

        return false
    }
}
