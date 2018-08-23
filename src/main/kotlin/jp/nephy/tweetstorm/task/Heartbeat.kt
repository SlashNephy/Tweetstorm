package jp.nephy.tweetstorm.task

import jp.nephy.tweetstorm.TaskManager
import java.util.concurrent.TimeUnit

class Heartbeat(override val manager: TaskManager): FetchTask<Nothing>() {
    override fun provide(data: Nothing) {}

    override fun fetch() {
        while (true) {
            manager.heartbeat()
            TimeUnit.SECONDS.sleep(10)
        }
    }
}
