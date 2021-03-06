package blue.starry.tweetstorm.task.producer

import blue.starry.tweetstorm.Config
import blue.starry.tweetstorm.task.ProduceTask
import blue.starry.tweetstorm.task.data.Heartbeat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlin.coroutines.CoroutineContext

class Heartbeat(account: Config.Account): ProduceTask<Heartbeat>(account) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce(context + parent) {
        while (isActive) {
            try {
                delay(10000)
                send(Heartbeat())
            } catch (e: CancellationException) {
                break
            }
        }
    }
}
