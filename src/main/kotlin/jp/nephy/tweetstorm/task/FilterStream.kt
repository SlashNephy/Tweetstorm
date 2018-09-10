package jp.nephy.tweetstorm.task

import com.google.gson.JsonObject
import jp.nephy.penicillin.core.streaming.FilterStreamListener
import jp.nephy.tweetstorm.TaskManager

class FilterStream(override val manager: TaskManager): RunnableTask() {
    private val stream = manager.twitter.stream.filter(track = manager.account.filterStreamTracks, follow = manager.account.filterStreamFollows).complete().listen(object: FilterStreamListener {
        override suspend fun onRawJson(json: JsonObject) {
            manager.emit(json)
        }

        override suspend fun onConnect() {
            logger.info { "Connected to FilterStream." }
        }

        override suspend fun onDisconnect() {
            logger.warn { "Disconnected from FilterStream." }
        }
    })

    override suspend fun run() {
        stream.start(wait = true, autoReconnect = true)
    }

    override fun close() {
        stream.close()
    }
}
