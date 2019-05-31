package jp.nephy.tweetstorm.producer.impl

import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.producer.data.JsonData
import jp.nephy.tweetstorm.producer.Producer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope

class Friends(private val target: AuthenticatedStream): Producer<JsonData>() {
    override val account: Config.Account
        get() = target.account

    private val stringifyFriendIds: Boolean
        get() = target.request.queryParameters["stringify_friend_ids"].orEmpty().toBooleanEasy()

    @ExperimentalCoroutinesApi
    override suspend fun ProducerScope<JsonData>.run() {
        if (stringifyFriendIds) {
            send(JsonData("friends_str" to account.friends.map { "$it" }))
        } else {
            send(JsonData("friends" to account.friends))
        }
    }

    private fun String.toBooleanEasy(): Boolean {
        return equals("true", true) || this == "1"
    }
}
