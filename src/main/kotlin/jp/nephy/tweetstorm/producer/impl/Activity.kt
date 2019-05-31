package jp.nephy.tweetstorm.producer.impl

import jp.nephy.jsonkt.edit
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.emulation.EmulationMode
import jp.nephy.penicillin.core.emulation.OfficialClient
import jp.nephy.penicillin.core.exceptions.PenicillinTwitterApiException
import jp.nephy.penicillin.core.exceptions.TwitterApiError
import jp.nephy.penicillin.core.session.config.*
import jp.nephy.penicillin.core.streaming.handler.UserStreamEvent
import jp.nephy.penicillin.core.streaming.handler.UserStreamEventType
import jp.nephy.penicillin.endpoints.activity
import jp.nephy.penicillin.endpoints.activity.aboutMe
import jp.nephy.penicillin.extensions.awaitWithTimeout
import jp.nephy.penicillin.extensions.createdAt
import jp.nephy.penicillin.extensions.duration
import jp.nephy.penicillin.extensions.rateLimit
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.Tweetstorm
import jp.nephy.tweetstorm.producer.Producer
import jp.nephy.tweetstorm.producer.data.JsonData
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.time.delay

class Activity(override val account: Config.Account): Producer<JsonData>() {
    @ExperimentalCoroutinesApi
    override suspend fun ProducerScope<JsonData>.run() {
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
                    it.activity.aboutMe().awaitWithTimeout(Tweetstorm.config.app.apiTimeout)
                } ?: continue

                if (activities.isNotEmpty()) {
                    val lastIdOrNull = if (lastId.value > 0) lastId.value else null
                    if (lastIdOrNull != null) {
                        activities.filter { json -> lastIdOrNull < json.createdAt.date.time }.reversed().forEach { event ->
                            val eventType = UserStreamEvent.byKey(event.action) ?: return@forEach
                            when (eventType.type) {
                                UserStreamEventType.Status, UserStreamEventType.List -> {
                                    val (source, targetObject) = event.sources.first().json to event.targets.first().json
                                    val target = targetObject.edit {
                                        it.remove("user")
                                    }
                                    send(
                                            JsonData(
                                                    "event" to eventType.key,
                                                    "source" to source,
                                                    "target" to target,
                                                    "target_object" to targetObject,
                                                    "created_at" to event.createdAt.value
                                            )
                                    )
                                }
                                UserStreamEventType.User -> {
                                    send(
                                            JsonData(
                                                    "event" to eventType.key,
                                                    "source" to event.sources.first().json,
                                                    "target" to event.targets.first().json,
                                                    "created_at" to event.createdAt.value
                                            )
                                    )
                                }
                            }
                        }
                    }

                    lastId.update {
                        activities.first().createdAt.date.time
                    }
                }

                val rateLimit = activities.rateLimit
                if (rateLimit != null) {
                    val duration = rateLimit.duration
                    if (rateLimit.remaining < 2) {
                        logger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${rateLimit.resetAt})" }
                        delay(duration)
                    } else if (duration.seconds > 3 && rateLimit.remaining * account.refresh.activity.toDouble() / duration.seconds / 1000 < 1) {
                        logger.warn { "Rate limit: API calls (/${activities.request.url}) seem to be frequent than expected so consider adjusting `activity_refresh` value in config.json. Sleep 10 secs. (${rateLimit.remaining}/${rateLimit.limit}, Reset at ${rateLimit.resetAt})" }
                        delay(10000)
                    }
                }

                delay(account.refresh.activity)
            } catch (e: CancellationException) {
                break
            } catch (e: PenicillinTwitterApiException) {
                if (e.error == TwitterApiError.RateLimitExceeded) {
                    delay(10000)
                } else {
                    logger.error(e) { "An error occurred while getting activities." }
                    delay(account.refresh.activity)
                }
            }
        }
    }
}
