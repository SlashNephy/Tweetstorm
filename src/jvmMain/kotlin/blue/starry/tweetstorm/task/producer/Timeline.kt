package blue.starry.tweetstorm.task.producer

import blue.starry.jsonkt.copy
import blue.starry.jsonkt.string
import blue.starry.penicillin.core.exceptions.PenicillinTwitterApiException
import blue.starry.penicillin.core.exceptions.TwitterApiError
import blue.starry.penicillin.core.request.action.JsonArrayApiAction
import blue.starry.penicillin.core.session.ApiClient
import blue.starry.penicillin.endpoints.common.TweetMode
import blue.starry.penicillin.endpoints.timeline
import blue.starry.penicillin.endpoints.timeline.homeTimeline
import blue.starry.penicillin.endpoints.timeline.listTimeline
import blue.starry.penicillin.endpoints.timeline.mentionsTimeline
import blue.starry.penicillin.endpoints.timeline.userTimeline
import blue.starry.penicillin.extensions.createdAt
import blue.starry.penicillin.extensions.executeWithTimeout
import blue.starry.penicillin.extensions.instant
import blue.starry.penicillin.extensions.rateLimit
import blue.starry.penicillin.models.Status
import blue.starry.tweetstorm.Config
import blue.starry.tweetstorm.config
import blue.starry.tweetstorm.task.ProduceTask
import blue.starry.tweetstorm.task.data.JsonObjectData
import io.ktor.util.date.toJvmDate
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

class ListTimeline(account: Config.Account, client: ApiClient, filter: (Status) -> Boolean = { true }): TimelineTask(account, account.refresh.listTimeline, {
    client.timeline.listTimeline(listId = account.listId!!, count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true, tweetMode = TweetMode.Extended)
}, filter)

class HomeTimeline(account: Config.Account, client: ApiClient, filter: (Status) -> Boolean = { true }): TimelineTask(account, account.refresh.homeTimeline, {
    client.timeline.homeTimeline(count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true, tweetMode = TweetMode.Extended)
}, filter)

class UserTimeline(account: Config.Account, client: ApiClient, filter: (Status) -> Boolean = { true }): TimelineTask(account, account.refresh.userTimeline, {
    client.timeline.userTimeline(count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true, tweetMode = TweetMode.Extended)
}, filter)

class MentionTimeline(account: Config.Account, client: ApiClient, filter: (Status) -> Boolean = { true }): TimelineTask(account, account.refresh.mentionTimeline, {
    client.timeline.mentionsTimeline(count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true, tweetMode = TweetMode.Extended)
}, filter)

abstract class TimelineTask(account: Config.Account, private val time: Long, private val source: (lastId: Long?) -> JsonArrayApiAction<Status>, private val filter: (Status) -> Boolean): ProduceTask<JsonObjectData>(account) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce(context + parent) {
        val lastId = AtomicLong(0L)
        while (isActive) {
            try {
                val lastIdOrNull = lastId.get().let { if (it > 0) it else null }
                val timeline = source(lastIdOrNull).executeWithTimeout(config.app.apiTimeout) ?: continue
                if (timeline.isNotEmpty()) {
                    if (lastIdOrNull != null) {
                        timeline.reversed().filter(filter).forEach {
                            send(JsonObjectData(it.postProcess()))
                        }
                    }

                    lastId.set(timeline.first().id)
                }

                val rateLimit = timeline.rateLimit
                if (rateLimit != null) {
                    val duration = Duration.between(Instant.now(), rateLimit.resetAt.toJvmDate().toInstant())
                    if (rateLimit.remaining < 2) {
                        logger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${rateLimit.resetAt})" }
                        delay(duration)
                    } else if (duration.seconds > 3 && rateLimit.remaining * time.toDouble() / duration.seconds < 1) {
                        logger.warn { "Rate limit: API calls (/${timeline.request.url}) seem to be frequent than expected so consider adjusting `*_timeline_refresh` value in config.json. Sleep 10 secs. (${rateLimit.remaining}/${rateLimit.limit}, Reset at ${rateLimit.resetAt})" }
                        delay(10000)
                    }
                    logger.trace { "Rate limit: ${rateLimit.remaining}/${rateLimit.limit}, Reset at ${rateLimit.resetAt}" }
                }

                delay(time)
            } catch (e: CancellationException) {
                break
            } catch (e: PenicillinTwitterApiException) {
                if (e.error == TwitterApiError.RateLimitExceeded) {
                    delay(10000)
                } else {
                    logger.error(e) { "An error occurred while getting timeline." }
                    delay(time)
                }
            }
        }
    }
}

private fun Status.postProcess() = json.copy {
    // For compatibility
    it["text"] = json["full_text"]!!.string
    it["truncated"] = false
    it.remove("display_text_range")
    it["quote_count"] = 0
    it["reply_count"] = 0
    it["possibly_sensitive"] = false
    it["filter_level"] = "low"
    it["timestamp_ms"] = createdAt.instant.epochSecond * 1000
}
