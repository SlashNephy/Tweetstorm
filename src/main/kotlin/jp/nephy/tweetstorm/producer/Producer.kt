package jp.nephy.tweetstorm.producer

import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.Tweetstorm
import jp.nephy.tweetstorm.logger
import jp.nephy.tweetstorm.producer.data.StreamData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

abstract class Producer<D: StreamData<*>>(interval: Long, unit: TimeUnit) {
    constructor(): this(0, TimeUnit.MILLISECONDS)

    private val interval = unit.toMillis(interval)

    abstract val account: Config.Account

    val logger by lazy {
        logger("Tweetstorm.task.${this::class.simpleName} (@${account.user.screenName})")
    }

    @ExperimentalCoroutinesApi
    abstract suspend fun ProducerScope<D>.run()

    @ExperimentalCoroutinesApi
    suspend fun start(job: Job): ReceiveChannel<D> {
        return Tweetstorm.produce(job) {
            logger.debug { "開始されました。" }

            while (job.isActive) {
                try {
                    run()
                } catch (e: CancellationException) {
                    break
                } catch (e: Throwable) {
                    logger.error(e) { "実行中にエラーが発生しました。" }
                }

                delay(interval)
            }

            logger.debug { "停止されました。" }
        }
    }
}
