package jp.nephy.tweetstorm.producer.impl

import jp.nephy.jsonkt.JsonObject
import jp.nephy.penicillin.core.session.ApiClient
import jp.nephy.penicillin.core.streaming.listener.FilterStreamListener
import jp.nephy.penicillin.endpoints.stream
import jp.nephy.penicillin.endpoints.stream.filter
import jp.nephy.penicillin.extensions.listen
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.producer.data.JsonObjectData
import jp.nephy.tweetstorm.producer.Producer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope

class FilterStream(override val account: Config.Account, private val client: ApiClient): Producer<JsonObjectData>() {
    @ExperimentalCoroutinesApi
    override suspend fun ProducerScope<JsonObjectData>.run() {
        client.stream.filter(track = account.filterStream.tracks, follow = account.filterStream.follows).listen(object: FilterStreamListener {
            override suspend fun onAnyJson(json: JsonObject) {
                send(JsonObjectData(json))
            }

            override suspend fun onConnect() {
                logger.info { "Connected to FilterStream." }
            }

            override suspend fun onDisconnect(cause: Throwable?) {
                logger.warn(cause) { "Disconnected from FilterStream." }
            }
        }).await(reconnect = true)
    }
}
