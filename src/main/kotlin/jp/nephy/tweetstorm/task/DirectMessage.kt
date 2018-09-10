package jp.nephy.tweetstorm.task

import jp.nephy.jsonkt.set
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.TwitterErrorMessage
import jp.nephy.tweetstorm.TaskManager
import jp.nephy.tweetstorm.builder.newDirectMessage
import jp.nephy.tweetstorm.config
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.time.delay
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class DirectMessage(override val manager: TaskManager): RunnableTask() {
    private var lastId: Long? = null
    override suspend fun run() {
        try {
            val messages = manager.twitter.directMessageEvent.list(count = 200).awaitWithTimeout(config.apiTimeoutSec, TimeUnit.SECONDS) ?: return
            if (messages.result.events.isNotEmpty()) {
                if (lastId != null) {
                    messages.result.events.asSequence().filter { it.type == "message_create" }.filter { lastId!! < it.id.toLong() }.toList().reversed().forEach {
                        manager.emit(newDirectMessage {
                            recipient {
                                json["id"] = it.messageCreate.target.recipientId.toLong()
                            }
                            sender {
                                json["id"] = it.messageCreate.senderId.toLong()
                            }
                            text { it.messageCreate.messageData.text }
                            json["entities"] = it.messageCreate.messageData.entities.json
                        })
                    }
                }

                lastId = messages.result.events.first().id.toLong()
            }

            if (messages.headers.rateLimit.hasLimit) {
                val duration = Duration.between(Instant.now(), messages.headers.rateLimit.resetAt!!.toInstant())
                if (messages.headers.rateLimit.remaining!! < 2) {
                    streamLogger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${messages.headers.rateLimit.resetAt})" }
                    delay(duration)
                } else if (duration.seconds > 3 && messages.headers.rateLimit.remaining!! * manager.account.messageInterval.toDouble() / duration.seconds < 1) {
                    streamLogger.warn { "Rate limit: API calls (/${messages.request.url}) seem to be frequent than expected so consider adjusting `direct_message_refresh_sec` value in config.json. Sleep 10 secs. (${messages.headers.rateLimit.remaining}/${messages.headers.rateLimit.limit}, Reset at ${messages.headers.rateLimit.resetAt})" }
                    delay(10, TimeUnit.SECONDS)
                }
            }

            delay(manager.account.messageInterval, TimeUnit.SECONDS)
        } catch (e: PenicillinException) {
            // Rate limit exceeded
            if (e.error == TwitterErrorMessage.RateLimitExceeded) {
                delay(10, TimeUnit.SECONDS)
            } else {
                logger.error(e) { "An error occurred while getting timeline." }
            }
        }
    }
}
