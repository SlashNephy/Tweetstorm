package blue.starry.tweetstorm.task.producer

import blue.starry.tweetstorm.session.AuthenticatedStream
import blue.starry.tweetstorm.task.TargetedProduceTask
import blue.starry.tweetstorm.task.data.JsonData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.produce
import kotlin.coroutines.CoroutineContext

class Friends(private val target: AuthenticatedStream): TargetedProduceTask<JsonData>(target) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce(context + parent) {
        val stringifyFriendIds = target.request.queryParameters["stringify_friend_ids"].orEmpty().toBooleanEasy()

        if (stringifyFriendIds) {
            send(JsonData("friends_str" to account.friends.map { "$it" }))
        } else {
            send(JsonData("friends" to account.friends))
        }
    }

    private fun String.toBooleanEasy(): Boolean {
        return equals("true", true) || equals("1")
    }
}
