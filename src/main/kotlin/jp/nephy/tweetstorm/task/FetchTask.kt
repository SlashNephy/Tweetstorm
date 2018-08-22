package jp.nephy.tweetstorm.task

import jp.nephy.tweetstorm.TaskManager
import jp.nephy.tweetstorm.session.AuthenticatedStream
import mu.KotlinLogging

abstract class Task {
    val logger by lazy { KotlinLogging.logger("Tweetstorm.task.${javaClass.simpleName} (${manager.account.displayName})") }
    abstract val manager: TaskManager
}

abstract class FetchTask<M>: Task() {
    abstract fun provide(data: M)
    abstract fun fetch()
}

abstract class TargetedFetchTask<M>: Task() {
    abstract fun provide(target: AuthenticatedStream, data: M)
    abstract fun fetch(target: AuthenticatedStream)
}
