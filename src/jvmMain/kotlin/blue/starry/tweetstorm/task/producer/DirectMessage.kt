package blue.starry.tweetstorm.task.producer

import blue.starry.penicillin.core.exceptions.PenicillinTwitterApiException
import blue.starry.penicillin.core.exceptions.TwitterApiError
import blue.starry.penicillin.core.session.ApiClient
import blue.starry.penicillin.endpoints.directMessageEvent
import blue.starry.penicillin.endpoints.directmessages.events.list
import blue.starry.penicillin.extensions.executeWithTimeout
import blue.starry.penicillin.extensions.models.builder.newDirectMessage
import blue.starry.penicillin.extensions.rateLimit
import blue.starry.tweetstorm.Config
import blue.starry.tweetstorm.config
import blue.starry.tweetstorm.task.ProduceTask
import blue.starry.tweetstorm.task.data.JsonModelData
import io.ktor.util.date.toJvmDate
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext

class DirectMessage(account: Config.Account, private val client: ApiClient): ProduceTask<JsonModelData>(account) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce(context + parent) {
        val lastId = AtomicLong()
        while (isActive) {
            try {
                val messages = client.directMessageEvent.list(count = 200).executeWithTimeout(config.app.apiTimeout) ?: continue
                if (messages.result.events.isNotEmpty()) {
                    val lastIdOrNull = lastId.get().let { if (it > 0) it else null }
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

                    lastId.set(messages.result.events.first().id.toLong())
                }

                val rateLimit = messages.rateLimit
                if (rateLimit != null) {
                    val duration = Duration.between(Instant.now(), rateLimit.resetAt.toJvmDate().toInstant())
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
