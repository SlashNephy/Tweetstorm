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
import kotlinx.atomicfu.update
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.time.delay
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext

class ListTimeline(account: Config.Account, filter: (Status) -> Boolean = { true }): TimelineTask(account, account.refresh.listTimeline, {
    account.twitter.list.timeline(listId = account.listId, count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true, tweetMode = "extended")
}, filter)

class HomeTimeline(account: Config.Account, filter: (Status) -> Boolean = { true }): TimelineTask(account, account.refresh.homeTimeline, {
    account.twitter.timeline.home(count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true, tweetMode = "extended")
}, filter)

class UserTimeline(account: Config.Account, filter: (Status) -> Boolean = { true }): TimelineTask(account, account.refresh.userTimeline, {
    account.twitter.timeline.user(count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true, tweetMode = "extended")
}, filter)

class MentionTimeline(account: Config.Account, filter: (Status) -> Boolean = { true }): TimelineTask(account, account.refresh.mentionTimeline, {
    account.twitter.timeline.mention(count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true, tweetMode = "extended")
}, filter)

abstract class TimelineTask(account: Config.Account, private val time: Long, private val source: (lastId: Long?) -> PenicillinJsonArrayAction<Status>, private val filter: (Status) -> Boolean): ProduceTask<JsonModelData>(account) {
    override fun channel(context: CoroutineContext, parent: Job) = produce(context, parent = parent) {
        val lastId = atomic(0L)
        while (isActive) {
            try {
                val lastIdOrNull = if (lastId.value > 0) lastId.value else null
                val timeline = source(lastIdOrNull).awaitWithTimeout(config.app.apiTimeout, TimeUnit.MILLISECONDS) ?: continue
                if (timeline.isNotEmpty()) {
                    if (lastIdOrNull != null) {
                        timeline.reversed().filter(filter).forEach {
                            send(JsonModelData(it.postProcess()))
                        }
                    }

                    lastId.update {
                        timeline.first().id
                    }
                }

                if (timeline.headers.rateLimit.hasLimit) {
                    val duration = Duration.between(Instant.now(), timeline.headers.rateLimit.resetAt!!.toInstant())
                    if (timeline.headers.rateLimit.remaining!! < 2) {
                        logger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${timeline.headers.rateLimit.resetAt})" }
                        delay(duration)
                    } else if (duration.seconds > 3 && timeline.headers.rateLimit.remaining!! * time.toDouble() / duration.seconds < 1) {
                        logger.warn { "Rate limit: API calls (/${timeline.request.url}) seem to be frequent than expected so consider adjusting `*_timeline_refresh` value in config.json. Sleep 10 secs. (${timeline.headers.rateLimit.remaining}/${timeline.headers.rateLimit.limit}, Reset at ${timeline.headers.rateLimit.resetAt})" }
                        delay(10, TimeUnit.SECONDS)
                    }
                    logger.trace { "Rate limit: ${timeline.headers.rateLimit.remaining}/${timeline.headers.rateLimit.limit}, Reset at ${timeline.headers.rateLimit.resetAt}" }
                }

                delay(time)
            } catch (e: CancellationException) {
                break
            } catch (e: PenicillinException) {
                if (e.error == TwitterErrorMessage.RateLimitExceeded) {
                    delay(10, TimeUnit.SECONDS)
                } else {
                    logger.error(e) { "An error occurred while getting timeline." }
                    delay(time)
                }
            }
        }
    }
}

private fun Status.postProcess() = apply {
    // For compatibility
    json["text"] = json.remove("full_text").string
    json["truncated"] = false
    json.remove("display_text_range")
    json["quote_count"] = 0
    json["reply_count"] = 0
    json["possibly_sensitive"] = false
    json["filter_level"] = "low"
    json["timestamp_ms"] = createdAt.date.time.toString()
}
