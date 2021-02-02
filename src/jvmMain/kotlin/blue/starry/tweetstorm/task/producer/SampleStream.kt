package blue.starry.tweetstorm.task.producer

import blue.starry.jsonkt.JsonObject
import blue.starry.penicillin.core.session.ApiClient
import blue.starry.penicillin.core.streaming.listener.SampleStreamListener
import blue.starry.penicillin.endpoints.stream
import blue.starry.penicillin.endpoints.stream.sample
import blue.starry.tweetstorm.Config
import blue.starry.tweetstorm.task.ProduceTask
import blue.starry.tweetstorm.task.data.JsonObjectData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.produce
import kotlin.coroutines.CoroutineContext

class SampleStream(account: Config.Account, private val client: ApiClient): ProduceTask<JsonObjectData>(account) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce<JsonObjectData>(context + parent) {
        client.stream.sample.listen(object: SampleStreamListener {
            override suspend fun onAnyJson(json: JsonObject) {
                send(JsonObjectData(json))
            }

            override suspend fun onConnect() {
                logger.info { "Connected to SampleStream." }
            }

            override suspend fun onDisconnect(cause: Throwable?) {
                logger.warn { "Disconnected from SampleStream." }
            }
        }, reconnect = true).join()
    }
}
