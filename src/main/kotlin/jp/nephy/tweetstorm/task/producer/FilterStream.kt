package jp.nephy.tweetstorm.task.producer

import jp.nephy.jsonkt.ImmutableJsonObject
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.streaming.FilterStreamListener
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.data.JsonObjectData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlin.coroutines.CoroutineContext

class FilterStream(account: Config.Account, private val client: PenicillinClient): ProduceTask<JsonObjectData>(account) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce(context + parent) {
        client.stream.filter(track = account.filterStream.tracks, follow = account.filterStream.follows).await().listen(object: FilterStreamListener {
            override suspend fun onAnyJson(json: ImmutableJsonObject) {
                send(JsonObjectData(json))
            }

            override suspend fun onConnect() {
                logger.info { "Connected to FilterStream." }
            }

            override suspend fun onDisconnect() {
                logger.warn { "Disconnected from FilterStream." }
            }
        }).startAsync(autoReconnect = true).use {
            while (isActive) {
                try {
                    delay(1000)
                } catch (e: CancellationException) {
                    break
                }
            }
        }
    }
}
