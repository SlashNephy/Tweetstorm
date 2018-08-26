package jp.nephy.tweetstorm

import io.ktor.features.origin
import io.ktor.util.toMap
import jp.nephy.tweetstorm.session.AuthenticatedStream
import java.util.concurrent.CopyOnWriteArrayList

class Fetcher {
    private val logger = logger("Tweetstorm.Fetcher")
    private val managers = CopyOnWriteArrayList<TaskManager>()

    fun start(stream: AuthenticatedStream) {
        logger.info { "Client: ${stream.account.fullName} (${stream.request.origin.remoteHost}) connected with parameter ${stream.request.queryParameters.toMap()}." }

        val manager = managers.find { it.account.id == stream.account.id }?.also {
            it.register(stream)
        } ?: TaskManager(stream).also {
            managers.add(it)
            it.start(stream)
        }

        manager.wait(stream)
        if (manager.shouldTerminate && managers.removeIf { it.account.id == stream.account.id }) {
            logger.debug { "Task Manager: ${manager.account.fullName} will terminate." }
            manager.close()
        }

        logger.info { "Client: ${stream.account.fullName} (${stream.request.origin.remoteHost}) has disconnected." }
    }
}
