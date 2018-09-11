package jp.nephy.tweetstorm.task.producer

import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.data.Heartbeat
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import java.util.concurrent.TimeUnit

class Heartbeat(account: Config.Account): ProduceTask<Heartbeat>(account) {
    override val channel = produce(capacity = Channel.UNLIMITED) {
        while (isActive) {
            try {
                delay(10, TimeUnit.SECONDS)
                send(Heartbeat())
            } catch (e: CancellationException) {
                break
            }
        }
    }
}
