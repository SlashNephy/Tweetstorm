package blue.starry.tweetstorm.task.regular

import blue.starry.penicillin.core.session.ApiClient
import blue.starry.penicillin.endpoints.friends
import blue.starry.penicillin.endpoints.friends.listIds
import blue.starry.penicillin.endpoints.lists
import blue.starry.penicillin.endpoints.lists.addMembersByUserIds
import blue.starry.penicillin.endpoints.lists.members
import blue.starry.penicillin.endpoints.lists.removeMembersByUserIds
import blue.starry.penicillin.extensions.cursor.allIds
import blue.starry.penicillin.extensions.cursor.allUsers
import blue.starry.penicillin.extensions.cursor.untilLast
import blue.starry.penicillin.extensions.execute
import blue.starry.tweetstorm.Config
import blue.starry.tweetstorm.task.RegularTask
import java.util.concurrent.TimeUnit

class SyncList(account: Config.Account, private val client: ApiClient): RegularTask(account, 5, TimeUnit.MINUTES) {
    override suspend fun run() {
        val followingIds = if (account.syncList.includeSelf) {
            client.friends.listIds(count = 5000).untilLast().allIds + account.user.id
        } else {
            client.friends.listIds(count = 5000).untilLast().allIds
        }

        if (followingIds.size > 5000) {
            logger.warn { "This list exceeded 5000 members limit." }
            return
        }
        val listMemberIds = client.lists.members(listId = account.listId!!, count = 5000).untilLast().allUsers.map { it.id }
        val willBeRemoved = listMemberIds - followingIds
        if (willBeRemoved.isNotEmpty()) {
            willBeRemoved.chunked(100).forEach {
                client.lists.removeMembersByUserIds(listId = account.listId!!, userIds = it).execute()
            }
            logger.debug { "Removing ${willBeRemoved.size} user(s)." }
        }
        val willBeAdded = followingIds - listMemberIds
        if (willBeAdded.isNotEmpty()) {
            willBeAdded.chunked(100).forEach {
                client.lists.addMembersByUserIds(listId = account.listId!!, userIds = it).execute()
            }
            logger.debug { "Adding ${willBeAdded.size} user(s)." }
        }
    }
}
