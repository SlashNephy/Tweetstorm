package jp.nephy.tweetstorm.task.producer

import jp.nephy.jsonkt.jsonObject
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.TwitterErrorMessage
import jp.nephy.penicillin.core.emulation.EmulationMode
import jp.nephy.penicillin.core.emulation.OfficialClient
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.builder.ListEventType
import jp.nephy.tweetstorm.builder.StatusEventType
import jp.nephy.tweetstorm.builder.UserEventType
import jp.nephy.tweetstorm.builder.toEventType
import jp.nephy.tweetstorm.config
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.data.JsonData
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

class Activity(account: Config.Account): ProduceTask<JsonData>(account) {
    private val twitter = PenicillinClient {
        account {
            application(OfficialClient.OAuth1a.TwitterForiPhone)
            token(account.t4i.at!!, account.t4i.ats!!)
        }
        emulate(EmulationMode.TwitterForiPhone)
        skipEmulationChecking()
    }

    override fun channel(context: CoroutineContext, parent: Job) = produce(context, parent = parent) {
        val lastId = atomic(0L)
        while (isActive) {
            try {
                val activities = twitter.activity.aboutMe().awaitWithTimeout(config.app.apiTimeout, TimeUnit.MILLISECONDS) ?: continue
                if (activities.isNotEmpty()) {
                    val lastIdOrNull = if (lastId.value > 0) lastId.value else null
                    if (lastIdOrNull != null) {
                        activities.filter { json -> lastIdOrNull < json.createdAt.date.time }.reversed().forEach { event ->
                            val type = event.action.toEventType() ?: return@forEach
                            when (type) {
                                is StatusEventType, is ListEventType -> {
                                    val (source, targetObject) = event.sources.first().json to event.targets.first().json
                                    val target = targetObject.remove("user").jsonObject
                                    send(
                                            JsonData(
                                                    "event" to type.key,
                                                    "source" to source,
                                                    "target" to target,
                                                    "target_object" to targetObject,
                                                    "created_at" to event.createdAt.value
                                            )
                                    )
                                }
                                is UserEventType -> {
                                    send(
                                            JsonData(
                                                    "event" to type.key,
                                                    "source" to event.sources.first().json,
                                                    "target" to event.targets.first().json,
                                                    "created_at" to event.createdAt.value
                                            )
                                    )
                                }
                                else -> {
                                    logger.warn { "Unknown EventType received: ${type.javaClass.canonicalName}" }
                                }
                            }
                        }
                    }

                    lastId.update {
                        activities.first().createdAt.date.time
                    }
                }

                if (activities.headers.rateLimit.hasLimit) {
                    val duration = Duration.between(Instant.now(), activities.headers.rateLimit.resetAt!!.toInstant())
                    if (activities.headers.rateLimit.remaining!! < 2) {
                        logger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${activities.headers.rateLimit.resetAt})" }
                        delay(duration)
                    } else if (duration.seconds > 3 && activities.headers.rateLimit.remaining!! * account.refresh.activity.toDouble() / duration.seconds / 1000 < 1) {
                        logger.warn { "Rate limit: API calls (/${activities.request.url}) seem to be frequent than expected so consider adjusting `activity_refresh` value in config.json. Sleep 10 secs. (${activities.headers.rateLimit.remaining}/${activities.headers.rateLimit.limit}, Reset at ${activities.headers.rateLimit.resetAt})" }
                        delay(10, TimeUnit.SECONDS)
                    }
                }

                delay(account.refresh.activity)
            } catch (e: CancellationException) {
                twitter.close()
                break
            } catch (e: PenicillinException) {
                if (e.error == TwitterErrorMessage.RateLimitExceeded) {
                    delay(10, TimeUnit.SECONDS)
                } else {
                    logger.error(e) { "An error occurred while getting activities." }
                    delay(account.refresh.activity)
                }
            }
        }
    }
}
