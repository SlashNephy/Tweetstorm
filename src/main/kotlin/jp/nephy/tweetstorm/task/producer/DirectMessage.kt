package jp.nephy.tweetstorm.task.producer

import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.TwitterErrorMessage
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.builder.newDirectMessage
import jp.nephy.tweetstorm.config
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.data.JsonModelData
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class DirectMessage(account: Config.Account): ProduceTask<JsonModelData>(account) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce(context + parent) {
        val lastId = atomic(0L)
        while (isActive) {
            try {
                val messages = account.twitter.directMessageEvent.list(count = 200).awaitWithTimeout(config.app.apiTimeout, TimeUnit.MILLISECONDS) ?: continue
                if (messages.result.events.isNotEmpty()) {
                    val lastIdOrNull = if (lastId.value > 0) lastId.value else null
                    if (lastIdOrNull != null) {
                        messages.result.events.asSequence().filter { it.type == "message_create" && lastIdOrNull < it.id.toLong() }.toList().reversed().forEach {
                            send(JsonModelData(newDirectMessage {
                                recipient {
                                    json["id"] = it.messageCreate.target.recipientId.toLong()
                                }
                                sender {
                                    json["id"] = it.messageCreate.senderId.toLong()
                                }
                                text { it.messageCreate.messageData.text }
                                json["entities"] = it.messageCreate.messageData.entities.json
                            }))
                        }
                    }

                    lastId.update {
                        messages.result.events.first().id.toLong()
                    }
                }

                if (messages.headers.rateLimit.hasLimit) {
                    val duration = Duration.between(Instant.now(), messages.headers.rateLimit.resetAt!!.toInstant())
                    if (messages.headers.rateLimit.remaining!! < 2) {
                        logger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${messages.headers.rateLimit.resetAt})" }
                        delay(duration)
                    } else if (duration.seconds > 3 && messages.headers.rateLimit.remaining!! * account.refresh.directMessage.toDouble() / 1000 / duration.seconds < 1) {
                        logger.warn { "Rate limit: API calls (/${messages.request.url}) seem to be frequent than expected so consider adjusting `direct_message_refresh` value in config.json. Sleep 10 secs. (${messages.headers.rateLimit.remaining}/${messages.headers.rateLimit.limit}, Reset at ${messages.headers.rateLimit.resetAt})" }
                        delay(10000)
                    }
                }

                delay(account.refresh.directMessage)
            } catch (e: CancellationException) {
                break
            } catch (e: PenicillinException) {
                if (e.error == TwitterErrorMessage.RateLimitExceeded) {
                    delay(10000)
                } else {
                    logger.error(e) { "An error occurred while getting direct messages." }
                    delay(account.refresh.directMessage)
                }
            }
        }
    }
}
