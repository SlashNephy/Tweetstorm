package jp.nephy.tweetstorm.task

import jp.nephy.jsonkt.contains
import jp.nephy.jsonkt.set
import jp.nephy.jsonkt.string
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.PenicillinJsonArrayAction
import jp.nephy.penicillin.core.TwitterErrorMessage
import jp.nephy.penicillin.models.CardState
import jp.nephy.penicillin.models.Status
import jp.nephy.tweetstorm.TaskManager
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

abstract class TimelineTask(final override val manager: TaskManager): FetchTask() {
    val timelineOptions = if (manager.account.markVote) {
        arrayOf("include_entities" to "true", "include_rts" to "true", "tweet_mode" to "extended", "include_cards" to "1", "cards_platform" to "iPhone-13")
    } else {
        arrayOf("include_entities" to "true", "include_rts" to "true", "tweet_mode" to "extended")
    }

    private var lastId: Long? = null
    fun timeline(sleepSec: Int, source: (lastId: Long?) -> PenicillinJsonArrayAction<Status>) {
        try {
            val timeline = source(lastId).complete()
            if (timeline.isNotEmpty()) {
                if (lastId != null) {
                    timeline.reversed().forEach {
                        manager.emit(it.apply { postProcess() })
                    }
                }

                lastId = timeline.first().id
            }

            if (timeline.rateLimit.hasLimit) {
                val duration = Duration.between(Instant.now(), timeline.rateLimit.resetAt!!.toInstant())
                if (timeline.rateLimit.remaining!! < 2) {
                    streamLogger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${timeline.rateLimit.resetAt})" }
                    TimeUnit.SECONDS.sleep(duration.seconds)
                } else if (timeline.rateLimit.remaining!! * sleepSec.toDouble() / duration.seconds < 1) {
                    streamLogger.warn { "Rate limit: API calls (/${timeline.request.url}) seem to be frequent than expected so consider adjusting `*_timeline_refresh_sec` value in config.json. Sleep 10 secs. (${timeline.rateLimit.remaining}/${timeline.rateLimit.limit}, Reset at ${timeline.rateLimit.resetAt})" }
                    TimeUnit.SECONDS.sleep(10)
                }
            }
        } catch (e: Exception) {
            // Rate limit exceeded
            if (e is PenicillinException && e.error == TwitterErrorMessage.RateLimitExceeded) {
                TimeUnit.SECONDS.sleep(10)
            } else {
                logger.error(e) { "An error occurred while getting timeline." }
            }
        } finally {
            TimeUnit.SECONDS.sleep(sleepSec.toLong())
        }
    }

    private fun Status.postProcess() {
        if (manager.account.markVia) {
            json["source"] = source.value.replace("</a>", " [Tweetstorm]</a>")
        }
        if (manager.account.markVote && json.contains("card")) {
            try {
                val card = CardState(json)
                json["full_text"] = buildString {
                    appendln(json["full_text"].string)
                    appendln("[Vote] ${card.data.minutes} mins")
                    append(card.data.choices.map { "${it.key}: ${it.value} Pt" }.joinToString(" / "))
                }
            } catch (e: Exception) {
                logger.debug(e) { "Vote element could not be extracted." }
            }
        }

        // For compatibility
        json["text"] = json["full_text"].string
    }
}

class ListTimeline(manager: TaskManager): TimelineTask(manager) {
    override fun run() {
        timeline(manager.account.listInterval) {
            manager.twitter.list.timeline(listId = manager.account.listId, count = 200, sinceId = it, options = *timelineOptions)
        }
    }
}

class HomeTimeline(manager: TaskManager): TimelineTask(manager) {
    override fun run() {
        timeline(manager.account.homeInterval) {
            manager.twitter.timeline.home(count = 200, sinceId = it, options = *timelineOptions)
        }
    }
}

class UserTimeline(manager: TaskManager): TimelineTask(manager) {
    override fun run() {
        timeline(manager.account.userInterval) {
            manager.twitter.timeline.user(count = 200, sinceId = it, options = *timelineOptions)
        }
    }
}

class MentionTimeline(manager: TaskManager): TimelineTask(manager) {
    override fun run() {
        timeline(manager.account.mentionInterval) {
            manager.twitter.timeline.mention(count = 200, sinceId = it, options = *timelineOptions)
        }
    }
}
