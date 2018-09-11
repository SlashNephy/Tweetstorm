package jp.nephy.tweetstorm.task

import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.logger
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.task.data.ProduceData
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import java.util.concurrent.TimeUnit

abstract class Task(val account: Config.Account) {
    val logger by lazy { logger("Tweetstorm.task.${javaClass.simpleName} (@${account.user.screenName})") }
}

abstract class ProduceTask<D: ProduceData<*>>(account: Config.Account): Task(account) {
    abstract val channel: ReceiveChannel<D>
}

abstract class TargetedProduceTask<D: ProduceData<*>>(target: AuthenticatedStream): Task(target.account) {
    abstract val channel: ReceiveChannel<D>
}

abstract class RegularTask(account: Config.Account, val interval: Long, val unit: TimeUnit): Task(account) {
    abstract suspend fun run()
}
