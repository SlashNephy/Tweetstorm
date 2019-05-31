package jp.nephy.tweetstorm.producer.impl

import jp.nephy.penicillin.core.session.ApiClient
import jp.nephy.penicillin.endpoints.friends
import jp.nephy.penicillin.endpoints.friends.listIds
import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.endpoints.lists.addMembersByUserIds
import jp.nephy.penicillin.endpoints.lists.members
import jp.nephy.penicillin.endpoints.lists.removeMembersByUserIds
import jp.nephy.penicillin.extensions.await
import jp.nephy.penicillin.extensions.cursor.allIds
import jp.nephy.penicillin.extensions.cursor.allUsers
import jp.nephy.penicillin.extensions.cursor.untilLast
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.producer.data.EmptyData
import jp.nephy.tweetstorm.producer.Producer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import java.util.concurrent.TimeUnit

class SyncList(override val account: Config.Account, private val client: ApiClient): Producer<EmptyData>(5, TimeUnit.MINUTES) {
    @ExperimentalCoroutinesApi
    override suspend fun ProducerScope<EmptyData>.run() {
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
                client.lists.removeMembersByUserIds(listId = account.listId!!, userIds = it).await()
            }
            logger.debug { "Removing ${willBeRemoved.size} user(s)." }
        }
        val willBeAdded = followingIds - listMemberIds
        if (willBeAdded.isNotEmpty()) {
            willBeAdded.chunked(100).forEach {
                client.lists.addMembersByUserIds(listId = account.listId!!, userIds = it).await()
            }
            logger.debug { "Adding ${willBeAdded.size} user(s)." }
        }
    }
}
