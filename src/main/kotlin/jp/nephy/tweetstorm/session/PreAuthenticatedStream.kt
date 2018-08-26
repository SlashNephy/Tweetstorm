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
        private val tokens = CopyOnWriteArrayList<String>()

        fun check(token: String): Boolean {
            return tokens.remove(token)
        }

        private fun register(): String {
            return UUID.randomUUID().toString().toLowerCase().replace("-", "").also {
                tokens.add(it)
            }
        }
    }

    private val token = register()

    private var isSuccessBack = false
    val isSuccess: Boolean
        get() = isSuccessBack

    fun block() {
        while (isAlive) {
            if (!tokens.contains(token)) {
                isSuccessBack = true
                return
            }

            heartbeat()
            TimeUnit.SECONDS.sleep(1)
        }
        isSuccessBack = false
    }

    override fun handle() {
        val stringifyFriendIds = request.queryParameters["stringify_friend_ids"].orEmpty().toBooleanEasy()
        if (stringifyFriendIds) {
            send("{\"friends_str\":[]}")
        } else {
            send("{\"friends\":[]}")
        }

        TimeUnit.SECONDS.sleep(2)

        val json = JsonKt.toJsonString(CustomStatusBuilder.new {
            user {
                name("Tweetstorm Authenticator")
            }
            text { "To start streaming, access https://userstream.twitter.com/auth/$token" }
            url("https://userstream.twitter.com/auth/$token", 27, 95)
        })
        send(json)

        block()
    }
}
