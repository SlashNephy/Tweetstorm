package jp.nephy.tweetstorm.task.producer

import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.data.Heartbeat
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext

class Heartbeat(account: Config.Account): ProduceTask<Heartbeat>(account) {
    override fun channel(context: CoroutineContext, parent: Job) = produce(context, parent = parent) {
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
