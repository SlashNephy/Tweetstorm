package jp.nephy.tweetstorm.task.producer

import jp.nephy.jsonkt.edit
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.emulation.EmulationMode
import jp.nephy.penicillin.core.emulation.OfficialClient
import jp.nephy.penicillin.core.exceptions.PenicillinException
import jp.nephy.penicillin.core.exceptions.TwitterErrorMessage
import jp.nephy.penicillin.core.session.config.account
import jp.nephy.penicillin.core.session.config.api
import jp.nephy.penicillin.endpoints.activity
import jp.nephy.penicillin.extensions.awaitWithTimeout
import jp.nephy.penicillin.extensions.createdAt
import jp.nephy.penicillin.extensions.hasLimit
import jp.nephy.penicillin.extensions.models.builder.ListEventType
import jp.nephy.penicillin.extensions.models.builder.StatusEventType
import jp.nephy.penicillin.extensions.models.builder.UserEventType
import jp.nephy.penicillin.extensions.models.builder.toEventType
import jp.nephy.penicillin.extensions.rateLimit
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.config
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.data.JsonData
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class Activity(account: Config.Account): ProduceTask<JsonData>(account) {
    @ExperimentalCoroutinesApi
    override fun channel(context: CoroutineContext, parent: Job) = GlobalScope.produce(context + parent) {
        val lastId = atomic(0L)
        while (isActive) {
            try {
                val activities = PenicillinClient {
                    account {
                        application(OfficialClient.OAuth1a.TwitterForiPhone)
                        token(account.t4i.at!!, account.t4i.ats!!)
                    }
                    api {
                        emulationMode = EmulationMode.TwitterForiPhone
                        skipEmulationChecking()
                    }
                }.use {
                    it.activity.aboutMe().awaitWithTimeout(config.app.apiTimeout, TimeUnit.MILLISECONDS)
                } ?: continue

                if (activities.isNotEmpty()) {
                    val lastIdOrNull = if (lastId.value > 0) lastId.value else null
                    if (lastIdOrNull != null) {
                        activities.filter { json -> lastIdOrNull < json.createdAt.date.time }.reversed().forEach { event ->
                            val type = event.action.toEventType() ?: return@forEach
                            when (type) {
                                is StatusEventType, is ListEventType -> {
                                    val (source, targetObject) = event.sources.first().json to event.targets.first().json
                                    val target = targetObject.edit {
                                        it.remove("user")
                                    }
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

                if (activities.rateLimit.hasLimit) {
                    val duration = Duration.between(Instant.now(), activities.rateLimit.resetAt!!.toInstant())
                    if (activities.rateLimit.remaining!! < 2) {
                        logger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${activities.rateLimit.resetAt})" }
                        delay(duration)
                    } else if (duration.seconds > 3 && activities.rateLimit.remaining!! * account.refresh.activity.toDouble() / duration.seconds / 1000 < 1) {
                        logger.warn { "Rate limit: API calls (/${activities.request.url}) seem to be frequent than expected so consider adjusting `activity_refresh` value in config.json. Sleep 10 secs. (${activities.rateLimit.remaining}/${activities.rateLimit.limit}, Reset at ${activities.rateLimit.resetAt})" }
                        delay(10000)
                    }
                }

                delay(account.refresh.activity)
            } catch (e: CancellationException) {
                break
            } catch (e: PenicillinException) {
                if (e.error == TwitterErrorMessage.RateLimitExceeded) {
                    delay(10000)
                } else {
                    logger.error(e) { "An error occurred while getting activities." }
                    delay(account.refresh.activity)
                }
            }
        }
    }
}
