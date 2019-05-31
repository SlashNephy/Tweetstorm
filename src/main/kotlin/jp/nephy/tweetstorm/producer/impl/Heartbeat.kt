package jp.nephy.tweetstorm.producer.impl

import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.producer.Producer
import jp.nephy.tweetstorm.producer.data.Heartbeat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope

class Heartbeat(override val account: Config.Account): Producer<Heartbeat>() {
    @ExperimentalCoroutinesApi
    override suspend fun ProducerScope<Heartbeat>.run() {
        delay(10000)

        send(Heartbeat)
    }
}
