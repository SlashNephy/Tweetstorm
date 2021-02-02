package blue.starry.tweetstorm.session

import blue.starry.tweetstorm.Config
import blue.starry.tweetstorm.TaskManager
import blue.starry.tweetstorm.logger
import io.ktor.features.origin
import io.ktor.request.ApplicationRequest
import io.ktor.util.toMap
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.sync.withLock

private val logger = logger("Tweetstorm.AuthenticatedStream")

class AuthenticatedStream(channel: ByteWriteChannel,request: ApplicationRequest, val account: Config.Account): Stream<Unit>(channel, request) {
    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    override suspend fun await() {
        logger.info { "Client: @${account.user.screenName} (${request.origin.remoteHost}) connected to UserStream API with parameter ${request.queryParameters.toMap()}." }

        val manager = TaskManager.instances.find { it.account.user.id == account.user.id }?.also {
            it.register(this)
        } ?: TaskManager(this).also {
            TaskManager.mutex.withLock {
                TaskManager.instances += it
            }

            it.start(this)
        }

        manager.wait(this)

        if (!manager.anyClients()) {
            val removed = TaskManager.mutex.withLock {
                TaskManager.instances.removeIf { it.account.user.id == account.user.id }
            }
            if (removed) {
                logger.debug { "Task Manager: @${manager.account.user.screenName} will terminate." }
                manager.close()
            }
        }

        logger.info { "Client: @${account.user.screenName} (${request.origin.remoteHost}) has disconnected from UserStream API." }
    }
}
