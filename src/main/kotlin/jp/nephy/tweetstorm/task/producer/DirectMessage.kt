package jp.nephy.tweetstorm.task.producer

import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.exceptions.PenicillinException
import jp.nephy.penicillin.core.exceptions.TwitterErrorMessage
import jp.nephy.penicillin.endpoints.directMessageEvent
import jp.nephy.penicillin.extensions.awaitWithTimeout
import jp.nephy.penicillin.extensions.hasLimit
import jp.nephy.penicillin.extensions.models.builder.newDirectMessage
import jp.nephy.penicillin.extensions.models.builder.update
import jp.nephy.penicillin.extensions.rateLimit
import jp.nephy.tweetstorm.Config
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

class DirectMessage(account: Config.Account, private val client: PenicillinClient): ProduceTask<JsonModelData>(account) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce(context + parent) {
        val lastId = atomic(0L)
        while (isActive) {
            try {
                val messages = client.directMessageEvent.list(count = 200).awaitWithTimeout(config.app.apiTimeout, TimeUnit.MILLISECONDS) ?: continue
                if (messages.result.events.isNotEmpty()) {
                    val lastIdOrNull = if (lastId.value > 0) lastId.value else null
                    if (lastIdOrNull != null) {
                        messages.result.events.asSequence().filter { it.type == "message_create" && lastIdOrNull < it.id.toLong() }.toList().reversed().forEach { event ->
                            send(JsonModelData(newDirectMessage {
                                recipient {
                                    update { 
                                        it["id"] = event.messageCreate.target.recipientId.toLong()
                                    }
                                }
                                sender {
                                    update {
                                        it["id"] = event.messageCreate.senderId.toLong()
                                    }
                                }
                                text { event.messageCreate.messageData.text }
                                entities(event.messageCreate.messageData.entities.json)
                            }))
                        }
                    }

                    lastId.update {
                        messages.result.events.first().id.toLong()
                    }
                }

                if (messages.rateLimit.hasLimit) {
                    val duration = Duration.between(Instant.now(), messages.rateLimit.resetAt!!.toInstant())
                    if (messages.rateLimit.remaining!! < 2) {
                        logger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${messages.rateLimit.resetAt})" }
                        delay(duration)
                    } else if (duration.seconds > 3 && messages.rateLimit.remaining!! * account.refresh.directMessage.toDouble() / 1000 / duration.seconds < 1) {
                        logger.warn { "Rate limit: API calls (/${messages.request.url}) seem to be frequent than expected so consider adjusting `direct_message_refresh` value in config.json. Sleep 10 secs. (${messages.rateLimit.remaining}/${messages.rateLimit.limit}, Reset at ${messages.rateLimit.resetAt})" }
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
