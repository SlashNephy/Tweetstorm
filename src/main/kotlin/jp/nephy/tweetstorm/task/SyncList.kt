package jp.nephy.tweetstorm.task

import jp.nephy.penicillin.request.allIds
import jp.nephy.penicillin.request.allUsers
import jp.nephy.tweetstorm.TaskManager
import java.util.concurrent.TimeUnit

class SyncList(override val manager: TaskManager): RegularTask(5, TimeUnit.MINUTES) {
    override fun run() {
        val followingIds = if (manager.account.syncListIncludeSelf) {
            manager.twitter.friend.listIds().complete().untilLast().allIds + manager.account.id
        } else {
            manager.twitter.friend.listIds().complete().untilLast().allIds
        }

        if (followingIds.size > 3000) {
            logger.warn { "This list exceeded 3000 members limit." }
            return
        }

        val listMemberIds = manager.twitter.list.members(listId = manager.account.listId).complete().untilLast().allUsers.map { it.id }

        val willBeRemoved = listMemberIds.filter { it !in followingIds }
        if (willBeRemoved.isNotEmpty()) {
            willBeRemoved.chunked(100).forEach {
                manager.twitter.list.removeMembers(listId = manager.account.listId, userIds = it).queue()
            }
            logger.debug { "Removing ${willBeRemoved.size} user(s)." }
        }

        val willBeAdded = followingIds.filter { it !in listMemberIds }
        if (willBeAdded.isNotEmpty()) {
            willBeAdded.chunked(100).forEach {
                manager.twitter.list.addMembers(listId = manager.account.listId, userIds = it).queue()
            }
            logger.debug { "Adding ${willBeAdded.size} user(s)." }
        }
    }
}
