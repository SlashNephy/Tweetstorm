package blue.starry.tweetstorm.task.producer

import blue.starry.jsonkt.JsonObject
import blue.starry.penicillin.core.session.ApiClient
import blue.starry.penicillin.core.streaming.listener.FilterStreamListener
import blue.starry.penicillin.endpoints.stream
import blue.starry.penicillin.endpoints.stream.filter
import blue.starry.tweetstorm.Config
import blue.starry.tweetstorm.task.ProduceTask
import blue.starry.tweetstorm.task.data.JsonObjectData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.produce
import kotlin.coroutines.CoroutineContext

class FilterStream(account: Config.Account, private val client: ApiClient): ProduceTask<JsonObjectData>(account) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce<JsonObjectData>(context + parent) {
        client.stream.filter(track = account.filterStream.tracks, follow = account.filterStream.follows).listen(object: FilterStreamListener {
            override suspend fun onAnyJson(json: JsonObject) {
                send(JsonObjectData(json))
            }

            override suspend fun onConnect() {
                logger.info { "Connected to FilterStream." }
            }

            override suspend fun onDisconnect(cause: Throwable?) {
                logger.warn { "Disconnected from FilterStream." }
            }
        }, reconnect = true).join()
    }
}
