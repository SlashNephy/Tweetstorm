package jp.nephy.tweetstorm.task

import jp.nephy.tweetstorm.TaskManager
import jp.nephy.tweetstorm.logger
import jp.nephy.tweetstorm.session.AuthenticatedStream
import java.util.concurrent.TimeUnit

abstract class Task {
    val logger by lazy { logger("Tweetstorm.task.${javaClass.simpleName} (${manager.account.displayName})") }
    abstract val manager: TaskManager
}

abstract class FetchTask: Task() {
    abstract fun run()
}

abstract class TargetedFetchTask: Task() {
    abstract fun run(target: AuthenticatedStream)
}

abstract class RegularTask(val interval: Long, val unit: TimeUnit): Task() {
    abstract fun run()
}
