package jp.nephy.tweetstorm

import jp.nephy.tweetstorm.session.AuthenticatedStream
import java.util.concurrent.CopyOnWriteArrayList

class Fetcher {
    private val managers = CopyOnWriteArrayList<TaskManager>()

    fun start(stream: AuthenticatedStream) {
        val manager = managers.find { it.account.id == stream.account.id }?.also {
            it.register(stream)
        } ?: TaskManager(stream).also {
            it.startTasks()
            it.startTargetedTasks(stream)
            managers.add(it)
        }

        manager.wait(stream)
    }
}
