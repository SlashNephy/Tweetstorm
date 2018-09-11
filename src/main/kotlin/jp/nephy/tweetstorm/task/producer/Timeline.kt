package jp.nephy.tweetstorm.task.producer

import jp.nephy.jsonkt.set
import jp.nephy.jsonkt.string
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.PenicillinJsonArrayAction
import jp.nephy.penicillin.core.TwitterErrorMessage
import jp.nephy.penicillin.models.Status
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.config
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.data.JsonModelData
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.time.delay
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

private val timelineOptions = arrayOf("include_entities" to "true", "include_rts" to "true", "tweet_mode" to "extended")

class ListTimeline(account: Config.Account): TimelineTask(account, account.listInterval, {
    account.twitter.list.timeline(listId = account.listId, count = 200, sinceId = it, options = *timelineOptions)
})

class HomeTimeline(account: Config.Account): TimelineTask(account, account.homeInterval, {
    account.twitter.timeline.home(count = 200, sinceId = it, options = *timelineOptions)
})

class UserTimeline(account: Config.Account): TimelineTask(account, account.userInterval, {
    account.twitter.timeline.user(count = 200, sinceId = it, options = *timelineOptions)
})

class MentionTimeline(account: Config.Account): TimelineTask(account, account.mentionInterval, {
    account.twitter.timeline.mention(count = 200, sinceId = it, options = *timelineOptions)
})

abstract class TimelineTask(account: Config.Account, sleepSec: Long, source: (lastId: Long?) -> PenicillinJsonArrayAction<Status>): ProduceTask<JsonModelData>(account) {
    private val lastId = atomic(0L)
    override val channel = produce(capacity = Channel.UNLIMITED) {
        while (isActive) {
            try {
                val timeline = source(if (lastId.value > 0) lastId.value else null).awaitWithTimeout(config.apiTimeoutSec, TimeUnit.SECONDS) ?: continue
                if (timeline.isNotEmpty()) {
                    if (lastId.value > 0) {
                        timeline.reversed().forEach {
                            send(JsonModelData(it.apply { postProcess() }))
                        }
                    }

                    lastId.value = timeline.first().id
                }

                if (timeline.headers.rateLimit.hasLimit) {
                    val duration = Duration.between(Instant.now(), timeline.headers.rateLimit.resetAt!!.toInstant())
                    if (timeline.headers.rateLimit.remaining!! < 2) {
                        // streamLogger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${timeline.headers.rateLimit.resetAt})" }
                        delay(duration)
                    } else if (duration.seconds > 3 && timeline.headers.rateLimit.remaining!! * sleepSec.toDouble() / duration.seconds < 1) {
                        // streamLogger.warn { "Rate limit: API calls (/${timeline.request.url}) seem to be frequent than expected so consider adjusting `*_timeline_refresh_sec` value in config.json. Sleep 10 secs. (${timeline.headers.rateLimit.remaining}/${timeline.headers.rateLimit.limit}, Reset at ${timeline.headers.rateLimit.resetAt})" }
                        delay(10, TimeUnit.SECONDS)
                    }
                }

                delay(sleepSec, TimeUnit.SECONDS)
            } catch (e: CancellationException) {
                break
            } catch (e: PenicillinException) {
                if (e.error == TwitterErrorMessage.RateLimitExceeded) {
                    delay(10, TimeUnit.SECONDS)
                } else {
                    logger.error(e) { "An error occurred while getting timeline." }
                    delay(sleepSec, TimeUnit.SECONDS)
                }
            }
        }
    }

    private fun Status.postProcess() {
        // For compatibility
        json["text"] = json["full_text"].string
    }
}
