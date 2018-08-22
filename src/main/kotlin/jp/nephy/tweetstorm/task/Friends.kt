package jp.nephy.tweetstorm.task

import jp.nephy.tweetstorm.TaskManager
import jp.nephy.tweetstorm.session.AuthenticatedStream

class Friends(override val manager: TaskManager): TargetedFetchTask<List<Long>>() {
    override fun provide(target: AuthenticatedStream, data: List<Long>) {
        if (target.stringifyFriendIds) {
            manager.emit(target, "friends_str" to data.map { "$it" })
        } else {
            manager.emit(target, "friends" to data)
        }
    }

    override fun fetch(target: AuthenticatedStream) {
        try {
            provide(target, manager.twitter.friend.listIds().complete().untilLast().flatMap { it.result.ids })
        } catch (e: Exception) {
            logger.error(e) { "An error occurred while getting friend ids." }
        }
    }
}
