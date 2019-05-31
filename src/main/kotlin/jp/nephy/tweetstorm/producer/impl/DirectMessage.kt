package jp.nephy.tweetstorm.producer.impl

import jp.nephy.penicillin.core.exceptions.PenicillinTwitterApiException
import jp.nephy.penicillin.core.exceptions.TwitterApiError
import jp.nephy.penicillin.core.session.ApiClient
import jp.nephy.penicillin.endpoints.directMessageEvent
import jp.nephy.penicillin.endpoints.directmessages.events.list
import jp.nephy.penicillin.extensions.awaitWithTimeout
import jp.nephy.penicillin.extensions.duration
import jp.nephy.penicillin.extensions.models.builder.newDirectMessage
import jp.nephy.penicillin.extensions.rateLimit
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.Tweetstorm
import jp.nephy.tweetstorm.producer.data.JsonModelData
import jp.nephy.tweetstorm.producer.Producer
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.time.delay

class DirectMessage(override val account: Config.Account, private val client: ApiClient): Producer<JsonModelData>() {
    @ExperimentalCoroutinesApi
    override suspend fun ProducerScope<JsonModelData>.run() {
        val lastId = atomic(0L)
        while (isActive) {
            try {
                val messages = client.directMessageEvent.list(count = 200).awaitWithTimeout(Tweetstorm.config.app.apiTimeout) ?: continue
                if (messages.result.events.isNotEmpty()) {
                    val lastIdOrNull = if (lastId.value > 0) lastId.value else null
                    if (lastIdOrNull != null) {
                        messages.result.events.asSequence().filter { it.type == "message_create" && lastIdOrNull < it.id.toLong() }.toList().reversed().forEach { event ->
                            send(JsonModelData(newDirectMessage {
                                recipient {
                                    this["id"] = event.messageCreate.target.recipientId.toLong()
                                }
                                sender {
                                    this["id"] = event.messageCreate.senderId.toLong()
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

                val rateLimit = messages.rateLimit
                if (rateLimit != null) {
                    val duration = rateLimit.duration
                    if (rateLimit.remaining < 2) {
                        logger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${rateLimit.resetAt})" }
                        delay(duration)
                    } else if (duration.seconds > 3 && rateLimit.remaining * account.refresh.directMessage.toDouble() / 1000 / duration.seconds < 1) {
                        logger.warn { "Rate limit: API calls (/${messages.request.url}) seem to be frequent than expected so consider adjusting `direct_message_refresh` value in config.json. Sleep 10 secs. (${rateLimit.remaining}/${rateLimit.limit}, Reset at ${rateLimit.resetAt})" }
                        delay(10000)
                    }
                }

                delay(account.refresh.directMessage)
            } catch (e: CancellationException) {
                break
            } catch (e: PenicillinTwitterApiException) {
                if (e.error == TwitterApiError.RateLimitExceeded) {
                    delay(10000)
                } else {
                    logger.error(e) { "An error occurred while getting direct messages." }
                    delay(account.refresh.directMessage)
                }
            }
        }
    }
}
