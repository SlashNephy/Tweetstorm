package jp.nephy.tweetstorm.task

import jp.nephy.jsonkt.jsonObject
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.TwitterErrorMessage
import jp.nephy.penicillin.core.emulation.EmulationMode
import jp.nephy.penicillin.core.emulation.OfficialClient
import jp.nephy.tweetstorm.TaskManager
import jp.nephy.tweetstorm.builder.ListEventType
import jp.nephy.tweetstorm.builder.StatusEventType
import jp.nephy.tweetstorm.builder.UserEventType
import jp.nephy.tweetstorm.builder.toEventType
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class Activity(override val manager: TaskManager): RunnableTask() {
    private val sleepSec = manager.account.activityInterval.toLong()
    private val twitter = PenicillinClient {
        account {
            application(OfficialClient.OAuth1a.TwitterForiPhone)
            token(manager.account.twitterForiPhoneAccessToken!!, manager.account.twitterForiPhoneAccessTokenSecret!!)
        }
        emulate(EmulationMode.TwitterForiPhone)
    }

    private var lastId: Long? = null
    override fun run() {
        try {
            val activities = twitter.activity.aboutMe().complete()
            if (activities.isNotEmpty()) {
                if (lastId != null) {
                    activities.filter { lastId!! < it.createdAt.date.time }.reversed().forEach { event ->
                        logger.trace { "Event = $event" }
                        val type = event.action.toEventType() ?: return@forEach
                        when (type) {
                            is StatusEventType, is ListEventType -> {
                                val (source, targetObject) = event.sources.first().json to event.targets.first().json
                                val target = targetObject.remove("user").jsonObject
                                manager.emit(
                                        "event" to type.key,
                                        "source" to source,
                                        "target" to target,
                                        "target_object" to targetObject,
                                        "created_at" to event.createdAt.value
                                )
                            }
                            is UserEventType -> {
                                manager.emit(
                                        "event" to type.key,
                                        "source" to event.sources.first().json,
                                        "target" to event.targets.first().json,
                                        "created_at" to event.createdAt.value
                                )
                            }
                            else -> {
                                logger.warn { "Unknown EventType received: ${type.javaClass.canonicalName}" }
                            }
                        }
                    }
                }

                lastId = activities.first().createdAt.date.time
            }

            if (activities.headers.rateLimit.hasLimit) {
                val duration = Duration.between(Instant.now(), activities.headers.rateLimit.resetAt!!.toInstant())
                if (activities.headers.rateLimit.remaining!! < 2) {
                    streamLogger.warn { "Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${activities.headers.rateLimit.resetAt})" }
                    TimeUnit.SECONDS.sleep(duration.seconds)
                } else if (duration.seconds > 3 && activities.headers.rateLimit.remaining!! * sleepSec.toDouble() / duration.seconds < 1) {
                    streamLogger.warn { "Rate limit: API calls (/${activities.request.url}) seem to be frequent than expected so consider adjusting `activity_refresh_sec` value in config.json. Sleep 10 secs. (${activities.headers.rateLimit.remaining}/${activities.headers.rateLimit.limit}, Reset at ${activities.headers.rateLimit.resetAt})" }
                    TimeUnit.SECONDS.sleep(10)
                }
            }
        } catch (e: Exception) {
            if (e is PenicillinException && e.error == TwitterErrorMessage.RateLimitExceeded) {
                TimeUnit.SECONDS.sleep(10)
            } else {
                logger.error(e) { "An error occurred while getting activities." }
            }
        } finally {
            TimeUnit.SECONDS.sleep(sleepSec)
        }
    }

    override fun close() {
        twitter.close()
    }
}
