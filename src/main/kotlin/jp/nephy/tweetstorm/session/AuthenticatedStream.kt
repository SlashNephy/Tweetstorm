package jp.nephy.tweetstorm.session

import io.ktor.features.origin
import io.ktor.request.ApplicationRequest
import io.ktor.util.toMap
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.TaskManager
import jp.nephy.tweetstorm.logger
import kotlinx.coroutines.experimental.sync.withLock
import java.io.Writer

private val logger = logger("Tweetstorm.AuthenticatedStream")

class AuthenticatedStream(writer: Writer,request: ApplicationRequest, val account: Config.Account): Stream<Unit>(writer, request) {
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
