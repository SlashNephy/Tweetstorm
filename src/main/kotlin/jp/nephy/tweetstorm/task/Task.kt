package jp.nephy.tweetstorm.task

import jp.nephy.tweetstorm.TaskManager
import jp.nephy.tweetstorm.logger
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.session.StreamLogger
import java.io.Closeable
import java.util.concurrent.TimeUnit

abstract class Task: Closeable {
    val logger by lazy { logger("Tweetstorm.task.${javaClass.simpleName} (@${manager.account.user.screenName})") }
    val streamLogger by lazy { StreamLogger(manager, "Tweetstorm.task.${javaClass.simpleName} (@${manager.account.user.screenName})") }
    abstract val manager: TaskManager

    override fun close() {}
}

abstract class RunnableTask: Task() {
    abstract fun run()
}

abstract class TargetedFetchTask: Task() {
    abstract fun run(target: AuthenticatedStream)
}

abstract class RegularTask(val interval: Long, val unit: TimeUnit): Task() {
    abstract fun run()
}
