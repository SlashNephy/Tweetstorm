package jp.nephy.tweetstorm.task.producer

import jp.nephy.jsonkt.set
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.TwitterErrorMessage
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.builder.newDirectMessage
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

class DirectMessage(account: Config.Account): ProduceTask<JsonModelData>(account) {
    override fun channel(context: CoroutineContext, parent: Job) = produce(context, parent = parent) {
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
                        // streamLogger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${messages.headers.rateLimit.resetAt})" }
                        delay(duration)
                    } else if (duration.seconds > 3 && messages.headers.rateLimit.remaining!! * account.refresh.directMessage.toDouble() / 1000 / duration.seconds < 1) {
                        // streamLogger.warn { "Rate limit: API calls (/${messages.request.url}) seem to be frequent than expected so consider adjusting `direct_message_refresh_sec` value in config.json. Sleep 10 secs. (${messages.headers.rateLimit.remaining}/${messages.headers.rateLimit.limit}, Reset at ${messages.headers.rateLimit.resetAt})" }
                        delay(10, TimeUnit.SECONDS)
                    }
                }

                delay(account.refresh.directMessage)
            } catch (e: CancellationException) {
                break
            } catch (e: PenicillinException) {
                if (e.error == TwitterErrorMessage.RateLimitExceeded) {
                    delay(10, TimeUnit.SECONDS)
                } else {
                    logger.error(e) { "An error occurred while getting direct messages." }
                    delay(account.refresh.directMessage)
                }
            }
        }
    }
}
