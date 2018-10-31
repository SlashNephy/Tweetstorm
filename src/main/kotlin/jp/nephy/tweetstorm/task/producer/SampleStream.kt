package jp.nephy.tweetstorm.task.producer

import jp.nephy.jsonkt.ImmutableJsonObject
import jp.nephy.penicillin.core.streaming.SampleStreamListener
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.data.JsonObjectData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlin.coroutines.CoroutineContext

class SampleStream(account: Config.Account): ProduceTask<JsonObjectData>(account) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce(context + parent) {
        val stream = account.twitter.stream.sample().await().listen(object: SampleStreamListener {
            override suspend fun onAnyJson(json: ImmutableJsonObject) {
                send(JsonObjectData(json))
            }

            override suspend fun onConnect() {
                logger.info { "Connected to SampleStream." }
            }

            override suspend fun onDisconnect() {
                logger.warn { "Disconnected from SampleStream." }
            }
        }).startAsync(autoReconnect = true)

        while (isActive) {
            try {
                delay(1000)
            } catch (e: CancellationException) {
                stream.close()
                break
            }
        }
    }
}
