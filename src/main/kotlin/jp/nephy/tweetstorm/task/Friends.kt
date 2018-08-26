package jp.nephy.tweetstorm.task

import jp.nephy.penicillin.request.allIds
import jp.nephy.tweetstorm.TaskManager
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.toBooleanEasy

class Friends(override val manager: TaskManager): TargetedFetchTask() {
    override fun run(target: AuthenticatedStream) {
        val stringifyFriendIds = target.request.queryParameters["stringify_friend_ids"].orEmpty().toBooleanEasy()
        val data = manager.twitter.friend.listIds().complete().untilLast().allIds

        if (stringifyFriendIds) {
            manager.emit(target, "friends_str" to data.map { "$it" })
        } else {
            manager.emit(target, "friends" to data)
        }
    }
}
