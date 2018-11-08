package jp.nephy.tweetstorm.task.regular

import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.allIds
import jp.nephy.penicillin.core.allUsers
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.task.RegularTask
import java.util.concurrent.TimeUnit

class SyncList(account: Config.Account, private val client: PenicillinClient): RegularTask(account, 5, TimeUnit.MINUTES) {
    override suspend fun run() {
        val followingIds = if (account.syncList.includeSelf) {
            client.friend.listIds(count = 5000).untilLast().allIds + account.user.id
        } else {
            client.friend.listIds(count = 5000).untilLast().allIds
        }

        if (followingIds.size > 5000) {
            logger.warn { "This list exceeded 5000 members limit." }
            return
        }
        val listMemberIds = client.list.members(listId = account.listId, count = 5000).untilLast().allUsers.map { it.id }
        val willBeRemoved = listMemberIds - followingIds
        if (willBeRemoved.isNotEmpty()) {
            willBeRemoved.chunked(100).forEach {
                client.list.removeMembers(listId = account.listId, userIds = it).await()
            }
            logger.debug { "Removing ${willBeRemoved.size} user(s)." }
        }
        val willBeAdded = followingIds - listMemberIds
        if (willBeAdded.isNotEmpty()) {
            willBeAdded.chunked(100).forEach {
                client.list.addMembers(listId = account.listId, userIds = it).await()
            }
            logger.debug { "Adding ${willBeAdded.size} user(s)." }
        }
    }
}
