package jp.nephy.tweetstorm.task

import com.google.gson.JsonObject
import jp.nephy.penicillin.core.streaming.SampleStreamListener
import jp.nephy.tweetstorm.TaskManager

class SampleStream(override val manager: TaskManager): RunnableTask() {
    private val stream = manager.twitter.stream.sample().complete().listen(object: SampleStreamListener {
        override fun onRawJson(json: JsonObject) {
            manager.emit(json)
        }

        override fun onConnect() {
            logger.info { "Connected to SampleStream." }
        }

        override fun onDisconnect() {
            logger.warn { "Disconnected from SampleStream." }
        }
    })

    override fun run() {
        stream.start(wait = true, autoReconnect = true)
    }

    override fun close() {
        stream.close()
    }
}
