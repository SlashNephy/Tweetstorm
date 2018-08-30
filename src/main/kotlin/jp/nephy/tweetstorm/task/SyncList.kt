package jp.nephy.tweetstorm.task

import jp.nephy.penicillin.core.allIds
import jp.nephy.penicillin.core.allUsers
import jp.nephy.tweetstorm.TaskManager
import java.util.concurrent.TimeUnit

class SyncList(override val manager: TaskManager): RegularTask(5, TimeUnit.MINUTES) {
    override fun run() {
        val followingIds = if (manager.account.syncListIncludeSelf) {
            manager.twitter.friend.listIds(count = 5000).complete().untilLast().allIds + manager.account.id
        } else {
            manager.twitter.friend.listIds(count = 5000).complete().untilLast().allIds
        }

        if (followingIds.size > 5000) {
            logger.warn { "This list exceeded 5000 members limit." }
            return
        }

        val listMemberIds = manager.twitter.list.members(listId = manager.account.listId, count = 5000).complete().untilLast().allUsers.map { it.id }

        val willBeRemoved = listMemberIds - followingIds
        if (willBeRemoved.isNotEmpty()) {
            willBeRemoved.chunked(100).forEach {
                manager.twitter.list.removeMembers(listId = manager.account.listId, userIds = it).complete()
            }
            logger.debug { "Removing ${willBeRemoved.size} user(s)." }
        }

        val willBeAdded = followingIds - listMemberIds
        if (willBeAdded.isNotEmpty()) {
            willBeAdded.chunked(100).forEach {
                manager.twitter.list.addMembers(listId = manager.account.listId, userIds = it).complete()
            }
            logger.debug { "Adding ${willBeAdded.size} user(s)." }
        }
    }
}
