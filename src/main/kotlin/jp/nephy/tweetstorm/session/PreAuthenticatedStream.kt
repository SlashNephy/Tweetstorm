package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest
import jp.nephy.jsonkt.JsonKt
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.builder.CustomStatusBuilder
import jp.nephy.tweetstorm.toBooleanEasy
import java.io.Writer
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class PreAuthenticatedStream(writer: Writer, override val request: ApplicationRequest, val account: Config.Account): StreamWriter(writer) {
    companion object {
        private val streams = CopyOnWriteArrayList<PreAuthenticatedStream>()

        @Synchronized
        fun auth(urlToken: String, accountToken: String): Boolean {
            return streams.removeIf { it.urlToken == urlToken && it.account.token == accountToken }
        }

        @Synchronized
        private fun contain(stream: PreAuthenticatedStream): Boolean {
            return streams.count { it.urlToken == stream.urlToken && it.account.token == stream.account.token } > 0
        }

        @Synchronized
        private fun register(stream: PreAuthenticatedStream) {
            streams.add(stream)
        }
    }

    private val urlToken = UUID.randomUUID().toString().toLowerCase().replace("-", "")

    private var isSuccessBack = false
    val isSuccess: Boolean
        get() = isSuccessBack

    override fun handle() {
        register(this)

        val stringifyFriendIds = request.queryParameters["stringify_friend_ids"].orEmpty().toBooleanEasy()
        if (stringifyFriendIds) {
            send("{\"friends_str\":[]}")
        } else {
            send("{\"friends\":[]}")
        }

        TimeUnit.SECONDS.sleep(3)

        heartbeat()

        send(JsonKt.toJsonString(CustomStatusBuilder.new {
            user {
                name("Tweetstorm Authenticator")
            }
            text { "To start streaming, access https://userstream.twitter.com/auth/token/$urlToken" }
            url("https://userstream.twitter.com/auth/token/$urlToken", 27, 101)
        }))

        repeat(300) {
            if (!contain(this)) {
                isSuccessBack = true
                return
            }

            if (it % 10 == 0) {
                heartbeat()
            }
            TimeUnit.SECONDS.sleep(1)
        }
    }
}
