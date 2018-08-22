package jp.nephy.tweetstorm

import jp.nephy.tweetstorm.session.AuthenticatedStream
import java.util.concurrent.CopyOnWriteArrayList

class Fetcher {
    private val managers = CopyOnWriteArrayList<TaskManager>()

    fun start(stream: AuthenticatedStream) {
        val manager = managers.find { it.account.id == stream.account.id }
        if (manager == null) {
            managers.add(
                    TaskManager(stream.account, stream).apply {
                        startTasks()
                        startTargetedTasks(stream)
                    }
            )
            return
        }

        manager.bind(stream)
    }
}
