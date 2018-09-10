package jp.nephy.tweetstorm.task

import jp.nephy.tweetstorm.TaskManager
import kotlinx.coroutines.experimental.delay
import java.util.concurrent.TimeUnit

class Heartbeat(override val manager: TaskManager): RunnableTask() {
    override suspend fun run() {
        manager.heartbeat()
        delay(5, TimeUnit.SECONDS)
    }
}
