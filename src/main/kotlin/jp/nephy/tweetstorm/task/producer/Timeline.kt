package jp.nephy.tweetstorm.task.producer

import jp.nephy.jsonkt.edit
import jp.nephy.jsonkt.string
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.exceptions.PenicillinException
import jp.nephy.penicillin.core.exceptions.TwitterErrorMessage
import jp.nephy.penicillin.core.request.action.JsonArrayApiAction
import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.extensions.awaitWithTimeout
import jp.nephy.penicillin.extensions.createdAt
import jp.nephy.penicillin.extensions.hasLimit
import jp.nephy.penicillin.extensions.rateLimit
import jp.nephy.penicillin.models.Status
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.config
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.data.JsonObjectData
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class ListTimeline(account: Config.Account, client: PenicillinClient, filter: (Status) -> Boolean = { true }): TimelineTask(account, account.refresh.listTimeline, {
    client.lists.timeline(listId = account.listId!!, count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true, tweetMode = "extended")
}, filter)

class HomeTimeline(account: Config.Account, client: PenicillinClient, filter: (Status) -> Boolean = { true }): TimelineTask(account, account.refresh.homeTimeline, {
    client.timeline.home(count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true, tweetMode = "extended")
}, filter)

class UserTimeline(account: Config.Account, client: PenicillinClient, filter: (Status) -> Boolean = { true }): TimelineTask(account, account.refresh.userTimeline, {
    client.timeline.user(count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true, tweetMode = "extended")
}, filter)

class MentionTimeline(account: Config.Account, client: PenicillinClient, filter: (Status) -> Boolean = { true }): TimelineTask(account, account.refresh.mentionTimeline, {
    client.timeline.mention(count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true, tweetMode = "extended")
}, filter)

abstract class TimelineTask(account: Config.Account, private val time: Long, private val source: (lastId: Long?) -> JsonArrayApiAction<Status>, private val filter: (Status) -> Boolean): ProduceTask<JsonObjectData>(account) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce(context + parent) {
        val lastId = atomic(0L)
        while (isActive) {
            try {
                val lastIdOrNull = if (lastId.value > 0) lastId.value else null
                val timeline = source(lastIdOrNull).awaitWithTimeout(config.app.apiTimeout, TimeUnit.MILLISECONDS) ?: continue
                if (timeline.isNotEmpty()) {
                    if (lastIdOrNull != null) {
                        timeline.reversed().filter(filter).forEach {
                            send(JsonObjectData(it.postProcess()))
                        }
                    }

                    lastId.update {
                        timeline.first().id
                    }
                }

                if (timeline.rateLimit.hasLimit) {
                    val duration = Duration.between(Instant.now(), timeline.rateLimit.resetAt!!.toInstant())
                    if (timeline.rateLimit.remaining!! < 2) {
                        logger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${timeline.rateLimit.resetAt})" }
                        delay(duration)
                    } else if (duration.seconds > 3 && timeline.rateLimit.remaining!! * time.toDouble() / duration.seconds < 1) {
                        logger.warn { "Rate limit: API calls (/${timeline.request.url}) seem to be frequent than expected so consider adjusting `*_timeline_refresh` value in config.json. Sleep 10 secs. (${timeline.rateLimit.remaining}/${timeline.rateLimit.limit}, Reset at ${timeline.rateLimit.resetAt})" }
                        delay(10000)
                    }
                    logger.trace { "Rate limit: ${timeline.rateLimit.remaining}/${timeline.rateLimit.limit}, Reset at ${timeline.rateLimit.resetAt}" }
                }

                delay(time)
            } catch (e: CancellationException) {
                break
            } catch (e: PenicillinException) {
                if (e.error == TwitterErrorMessage.RateLimitExceeded) {
                    delay(10000)
                } else {
                    logger.error(e) { "An error occurred while getting timeline." }
                    delay(time)
                }
            }
        }
    }
}

private fun Status.postProcess() = json.edit {
    // For compatibility
    it["text"] = json["full_text"].string
    it["truncated"] = false
    it.remove("display_text_range")
    it["quote_count"] = 0
    it["reply_count"] = 0
    it["possibly_sensitive"] = false
    it["filter_level"] = "low"
    it["timestamp_ms"] = createdAt.date.time.toString()
}
