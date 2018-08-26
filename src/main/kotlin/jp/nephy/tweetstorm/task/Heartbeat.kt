package jp.nephy.tweetstorm.task

import jp.nephy.tweetstorm.TaskManager
import java.util.concurrent.TimeUnit

class Heartbeat(override val manager: TaskManager): FetchTask() {
    override fun run() {
        manager.heartbeat()
        TimeUnit.SECONDS.sleep(10)
    }
}
