package jp.nephy.tweetstorm

import io.ktor.util.cio.ChannelWriteException
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.set
import jp.nephy.jsonkt.string
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.PenicillinException
import jp.nephy.penicillin.model.Status
import jp.nephy.penicillin.request.ListAction
import jp.nephy.tweetstorm.session.AuthenticatedStream
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Fetcher {
    private val logger = KotlinLogging.logger("Tweetstorm.Fetcher")
    private val executor = Executors.newCachedThreadPool()
    private val tasks = CopyOnWriteArrayList<Fetcher.Task>()

    fun startIfNotRunning(stream: AuthenticatedStream) {
        val task = tasks.find { it.account.id == stream.account.id }
        if (task == null) {
            tasks.add(Task(stream.account, stream))
            return
        }

        task.bind(stream)
    }

    inner class Task(val account: Config.Account, initialStream: AuthenticatedStream) {
        private val client = PenicillinClient.build {
            application(account.ck, account.cs)
            token(account.at, account.ats)
        }
        private val streams = CopyOnWriteArrayList<AuthenticatedStream>().also {
            it.add(initialStream)
        }

        fun bind(stream: AuthenticatedStream) {
            if (stream !in streams) {
                streams.add(stream)
                executor.execute { friends(stream) }
                logger.info { "A Stream has been bind to @${account.sn}." }
            }
        }

        fun unbind(stream: AuthenticatedStream) {
            if (stream in streams) {
                streams.remove(stream)
                logger.info { "A Stream has been unbind from @${account.sn}." }
            }
        }

        private fun sendAll(payload: JsonModel) {
            for (it in streams) {
                try {
                    it.send(payload)
                } catch (e: ChannelWriteException) {
                    unbind(it)
                } catch (e: Exception) {
                    logger.error(e) { "A Stream failed sending payload. (@${account.sn})" }
                    continue
                }
            }
        }

        init {
            executor.execute {
                if (account.listId != null) {
                    listTimeline()
                } else {
                    homeTimeline()
                }
            }
            executor.execute {
                if (account.listId == null) {
                    return@execute
                }

                try {
                    client.list.member(listId = account.listId, userId = account.id).complete()
                } catch (e: Exception) {
                    // if list does not contain me
                    executor.execute { userTimeline() }
                    executor.execute { mentionTimeline() }
                    return@execute
                }

            }
            executor.execute { directMessage() }
            executor.execute { activity() }
            executor.execute { streams.forEach { friends(it) } }
            executor.execute { delete() }
            logger.info { "FetchTask: @${account.sn} has started." }
        }

        private fun listTimeline() {
            timeline(account.listInterval) {
                client.list.timeline(listId = account.listId, count = 200, sinceId = it, includeRTs = true, includeEntities = true, options = *arrayOf("tweet_mode" to "extended"))
            }
        }

        private fun homeTimeline() {
            timeline(account.homeInterval) {
                client.timeline.home(count = 200, sinceId = it, includeEntities = true, options = *arrayOf("tweet_mode" to "extended"))
            }
        }

        private fun userTimeline() {
            timeline(account.userInterval) {
                client.timeline.user(count = 200, sinceId = it, includeRTs = true, options = *arrayOf("tweet_mode" to "extended"))
            }
        }

        private fun mentionTimeline() {
            timeline(account.mentionInterval) {
                client.timeline.mention(count = 200, sinceId = it, includeEntities = true, options = *arrayOf("tweet_mode" to "extended"))
            }
        }

        private fun timeline(sleepSec: Int, source: (lastId: Long?) -> ListAction<Status>) {
            var lastId: Long? = null
            while (streams.isNotEmpty()) {
                try {
                    val timeline = source(lastId).complete()
                    if (timeline.result.isNotEmpty()) {
                        // For compatibility
                        timeline.result.forEach {
                            it.json["text"] = it.json["full_text"].string
                        }

                        if (lastId != null) {
                            timeline.result.reversed().forEach {
                                if (account.markVia) {
                                    it.json["source"] = it.source.value.replace("</a>", " [Tweetstorm]</a>")
                                }
                                sendAll(it)
                            }
                        }

                        lastId = timeline.result.first().id
                    }

                    if (timeline.rateLimit.hasLimit) {
                        val duration = Duration.between(Instant.now(), timeline.rateLimit.resetAt!!.toInstant())
                        if (timeline.rateLimit.remaining!! < 2) {
                            logger.warn { "[Timeline] Rate limit: Mostly exceeded. Sleep ${duration.seconds} secs. (Reset at ${timeline.rateLimit.resetAt})" }
                            TimeUnit.SECONDS.sleep(duration.seconds)
                        } else if (timeline.rateLimit.remaining!! * sleepSec.toDouble() / duration.seconds < 1) {
                            logger.warn { "[Timeline] Rate limit: API calls (/${timeline.request.url().pathSegments().joinToString("/")}) seem to be frequent than expected so consider adjusting `*_timeline_refresh_sec` value in config.json. Sleep 10 secs. (${timeline.rateLimit.remaining}/${timeline.rateLimit.limit}, Reset at ${timeline.rateLimit.resetAt})" }
                            TimeUnit.SECONDS.sleep(10)
                        }
                    }
                } catch (e: Exception) {
                    // Rate limit exceeded
                    if (e is PenicillinException && e.error?.code == 88) {
                        TimeUnit.SECONDS.sleep(10)
                    } else {
                        logger.error(e) { "[Timeline] An error occurred while getting timeline for @${account.sn}." }
                    }
                } finally {
                    TimeUnit.SECONDS.sleep(sleepSec.toLong())
                }
            }
        }

        private fun directMessage() {
            // TODO
        }

        private fun activity() {
            // TODO
        }

        private fun friends(target: AuthenticatedStream) {
            val friends = try {
                client.friend.listIds().complete().untilLast().flatMap { it.result.ids }
            } catch (e: Exception) {
                logger.error(e) { "[Friends] An error occurred while getting friend ids for @${account.sn}." }
                return
            }

            if (target.stringifyFriendIds) {
                target.send("friends_str" to friends.map { "$it" })
            } else {
                target.send("friends" to friends)
            }
        }

        private fun delete() {
            // TODO
        }
    }
}
