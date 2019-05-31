package jp.nephy.tweetstorm.producer.impl

import jp.nephy.jsonkt.edit
import jp.nephy.jsonkt.string
import jp.nephy.penicillin.core.exceptions.PenicillinTwitterApiException
import jp.nephy.penicillin.core.exceptions.TwitterApiError
import jp.nephy.penicillin.core.request.action.JsonArrayApiAction
import jp.nephy.penicillin.core.session.ApiClient
import jp.nephy.penicillin.endpoints.timeline
import jp.nephy.penicillin.endpoints.timeline.homeTimeline
import jp.nephy.penicillin.endpoints.timeline.listTimeline
import jp.nephy.penicillin.endpoints.timeline.mentionsTimeline
import jp.nephy.penicillin.endpoints.timeline.userTimeline
import jp.nephy.penicillin.extensions.awaitWithTimeout
import jp.nephy.penicillin.extensions.createdAt
import jp.nephy.penicillin.extensions.duration
import jp.nephy.penicillin.extensions.rateLimit
import jp.nephy.penicillin.models.Status
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.Tweetstorm
import jp.nephy.tweetstorm.producer.data.JsonObjectData
import jp.nephy.tweetstorm.producer.Producer
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.time.delay

class ListTimeline(account: Config.Account, client: ApiClient, filter: (Status) -> Boolean = { true }): TimelineProducer(account, account.refresh.listTimeline, {
    client.timeline.listTimeline(listId = account.listId!!, count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true)
}, filter)

class HomeTimeline(account: Config.Account, client: ApiClient, filter: (Status) -> Boolean = { true }): TimelineProducer(account, account.refresh.homeTimeline, {
    client.timeline.homeTimeline(count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true)
}, filter)

class UserTimeline(account: Config.Account, client: ApiClient, filter: (Status) -> Boolean = { true }): TimelineProducer(account, account.refresh.userTimeline, {
    client.timeline.userTimeline(count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true)
}, filter)

class MentionTimeline(account: Config.Account, client: ApiClient, filter: (Status) -> Boolean = { true }): TimelineProducer(account, account.refresh.mentionTimeline, {
    client.timeline.mentionsTimeline(count = 200, sinceId = it, includeEntities = true, includeRTs = true, includeMyRetweet = true)
}, filter)

abstract class TimelineProducer(override val account: Config.Account, private val time: Long, private val source: (lastId: Long?) -> JsonArrayApiAction<Status>, private val filter: (Status) -> Boolean): Producer<JsonObjectData>() {
    @ExperimentalCoroutinesApi
    override suspend fun ProducerScope<JsonObjectData>.run() {
        val lastId = atomic(0L)

        while (isActive) {
            try {
                val lastIdOrNull = if (lastId.value > 0) lastId.value else null
                val timeline = source(lastIdOrNull).awaitWithTimeout(Tweetstorm.config.app.apiTimeout) ?: continue
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

                val rateLimit = timeline.rateLimit
                if (rateLimit != null) {
                    val duration = rateLimit.duration
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

private fun Status.postProcess() = json.edit {
    // For compatibility
    it["text"] = json["full_text"]!!.string
    it["truncated"] = false
    it.remove("display_text_range")
    it["quote_count"] = 0
    it["reply_count"] = 0
    it["possibly_sensitive"] = false
    it["filter_level"] = "low"
    it["timestamp_ms"] = createdAt.date.time.toString()
}
