package jp.nephy.tweetstorm.task.producer

import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.task.TargetedProduceTask
import jp.nephy.tweetstorm.task.data.JsonData
import jp.nephy.tweetstorm.toBooleanEasy
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.produce
import kotlin.coroutines.experimental.CoroutineContext

class Friends(private val target: AuthenticatedStream): TargetedProduceTask<JsonData>(target) {
    override fun channel(context: CoroutineContext, parent: Job) = produce(context, parent = parent) {
        val stringifyFriendIds = target.request.queryParameters["stringify_friend_ids"].orEmpty().toBooleanEasy()

        if (stringifyFriendIds) {
            send(JsonData("friends_str" to account.friends.map { "$it" }))
        } else {
            send(JsonData("friends" to account.friends))
        }
    }
}
