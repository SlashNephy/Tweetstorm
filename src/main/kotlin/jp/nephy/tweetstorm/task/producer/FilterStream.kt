package jp.nephy.tweetstorm.task.producer

import jp.nephy.jsonkt.ImmutableJsonObject
import jp.nephy.penicillin.core.streaming.FilterStreamListener
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.data.JsonObjectData
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext

class FilterStream(account: Config.Account): ProduceTask<JsonObjectData>(account) {
    override fun channel(context: CoroutineContext, parent: Job) = produce(context, parent = parent) {
        val stream = account.twitter.stream.filter(track = account.filterStream.tracks, follow = account.filterStream.follows).await().listen(object: FilterStreamListener {
            override suspend fun onAnyJson(json: ImmutableJsonObject) {
                send(JsonObjectData(json))
            }

            override suspend fun onConnect() {
                logger.info { "Connected to FilterStream." }
            }

            override suspend fun onDisconnect() {
                logger.warn { "Disconnected from FilterStream." }
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
