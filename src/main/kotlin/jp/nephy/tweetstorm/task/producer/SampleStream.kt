package jp.nephy.tweetstorm.task.producer

import jp.nephy.jsonkt.JsonObject
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.streaming.listener.SampleStreamListener
import jp.nephy.penicillin.endpoints.stream
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.data.JsonObjectData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.produce
import kotlin.coroutines.CoroutineContext

class SampleStream(account: Config.Account, private val client: PenicillinClient): ProduceTask<JsonObjectData>(account) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce(context + parent) {
        client.stream.sample().await().listen(object: SampleStreamListener {
            override suspend fun onAnyJson(json: JsonObject) {
                send(JsonObjectData(json))
            }

            override suspend fun onConnect() {
                logger.info { "Connected to SampleStream." }
            }

            override suspend fun onDisconnect() {
                logger.warn { "Disconnected from SampleStream." }
            }
        }).await(autoReconnect = true)
    }
}
