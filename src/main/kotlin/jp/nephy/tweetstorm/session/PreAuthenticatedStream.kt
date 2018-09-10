package jp.nephy.tweetstorm.session

import io.ktor.features.origin
import io.ktor.request.ApplicationRequest
import jp.nephy.jsonkt.jsonArray
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.builder.newStatus
import jp.nephy.tweetstorm.toBooleanEasy
import kotlinx.coroutines.experimental.delay
import java.io.IOException
import java.io.Writer
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

private val logger = jp.nephy.tweetstorm.logger("Tweetstorm.PreAuthenticatedStream")

class PreAuthenticatedStream(writer: Writer, request: ApplicationRequest, val account: Config.Account): Stream<Boolean>(writer, request) {
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

        try {
            val stringifyFriendIds = request.queryParameters["stringify_friend_ids"].orEmpty().toBooleanEasy()
            if (stringifyFriendIds) {
                handler.emit("friends_str" to jsonArray())
            } else {
                handler.emit("friends" to jsonArray())
            }

            delay(3, TimeUnit.SECONDS)

            handler.heartbeat()

            handler.emit(newStatus {
                user {
                    name("Tweetstorm Authenticator")
                }
                text { "To start streaming, access https://userstream.twitter.com/auth/token/$urlToken" }
                url("https://userstream.twitter.com/auth/token/$urlToken", 27, 101)
            })

            repeat(300) {
                if (!contain(this)) {
                    return true
                }

                if (it % 10 == 0) {
                    handler.heartbeat()
                }
                delay(1, TimeUnit.SECONDS)
            }
            return false
        } catch (e: IOException) {
            return false
        }
    }
}
