package blue.starry.tweetstorm.task

import blue.starry.tweetstorm.Config
import blue.starry.tweetstorm.logger
import blue.starry.tweetstorm.session.AuthenticatedStream
import blue.starry.tweetstorm.task.data.ProduceData
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

abstract class Task(val account: Config.Account) {
    val logger by lazy { logger("Tweetstorm.task.${javaClass.simpleName} (@${account.user.screenName})") }
}

abstract class ProduceTask<D: ProduceData<*>>(account: Config.Account): Task(account) {
    abstract fun channel(context: CoroutineContext, parent: Job): ReceiveChannel<D>
}

abstract class TargetedProduceTask<D: ProduceData<*>>(target: AuthenticatedStream): Task(target.account) {
    abstract fun channel(context: CoroutineContext, parent: Job): ReceiveChannel<D>
}

abstract class RegularTask(account: Config.Account, val interval: Long, val unit: TimeUnit): Task(account) {
    abstract suspend fun run()
}
