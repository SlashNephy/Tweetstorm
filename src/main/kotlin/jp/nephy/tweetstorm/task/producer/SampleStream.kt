package jp.nephy.tweetstorm.task.producer

import com.google.gson.JsonObject
import jp.nephy.penicillin.core.streaming.SampleStreamListener
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.data.JsonObjectData
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import java.util.concurrent.TimeUnit

class SampleStream(account: Config.Account): ProduceTask<JsonObjectData>(account) {
    override val channel = produce(capacity = Channel.UNLIMITED) {
        val stream = account.twitter.stream.sample().await().listen(object: SampleStreamListener {
            override suspend fun onRawJson(json: JsonObject) {
                send(JsonObjectData(json))
            }

            override suspend fun onConnect() {
                logger.info { "Connected to SampleStream." }
            }

            override suspend fun onDisconnect() {
                logger.warn { "Disconnected from SampleStream." }
            }
        }).start(wait = false, autoReconnect = true)

        while (isActive) {
            try {
                delay(1, TimeUnit.SECONDS)
            } catch (e: CancellationException) {
                stream.close()
                break
            }
        }
    }
}
