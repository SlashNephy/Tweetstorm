package jp.nephy.tweetstorm.producer.impl

import jp.nephy.jsonkt.JsonObject
import jp.nephy.penicillin.core.session.ApiClient
import jp.nephy.penicillin.core.streaming.listener.SampleStreamListener
import jp.nephy.penicillin.endpoints.stream
import jp.nephy.penicillin.endpoints.stream.sample
import jp.nephy.penicillin.extensions.listen
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.producer.data.JsonObjectData
import jp.nephy.tweetstorm.producer.Producer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope

class SampleStream(override val account: Config.Account, private val client: ApiClient): Producer<JsonObjectData>() {
    @ExperimentalCoroutinesApi
    override suspend fun ProducerScope<JsonObjectData>.run() {
        client.stream.sample.listen(object: SampleStreamListener {
            override suspend fun onAnyJson(json: JsonObject) {
                send(JsonObjectData(json))
            }

            override suspend fun onConnect() {
                logger.info { "Connected to SampleStream." }
            }

            override suspend fun onDisconnect(cause: Throwable?) {
                logger.warn(cause) { "Disconnected from SampleStream." }
            }
        }).await(reconnect = true)
    }
}
