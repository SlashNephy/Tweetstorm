package jp.nephy.tweetstorm

import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.set
import jp.nephy.jsonkt.string
import jp.nephy.penicillin.PenicillinClient
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
        val task = tasks.find { it.account == stream.account }
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

        private fun sendAll(model: JsonModel) {
            streams.forEach { it.send(model) }
        }

        init {
            executor.execute { if (account.listId != null) listTimeline() else homeTimeline() }
            executor.execute { directMessage() }
            executor.execute { activity() }
            executor.execute { streams.forEach { friends(it) } }
            executor.execute { delete() }
            logger.info { "FetchTask: @${account.sn} has started." }
        }

        private fun listTimeline() {
            timeline(1) {
                client.list.timeline(listId = account.listId, count = 200, sinceId = it, includeRTs = true, includeEntities = true, options = *arrayOf("tweet_mode" to "extended"))
            }
        }

        private fun homeTimeline() {
            timeline(60) {
                client.timeline.home(count = 200, sinceId = it, includeEntities = true, options = *arrayOf("tweet_mode" to "extended"))
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
                        logger.debug { "[Timeline] Rate limit: ${timeline.rateLimit.remaining}/${timeline.rateLimit.limit} (${timeline.rateLimit.resetAt})" }

                        val duration = Duration.between(Instant.now(), timeline.rateLimit.resetAt!!.toInstant())
                        if (timeline.rateLimit.remaining!! * sleepSec * 1.0 / duration.seconds < 1) {
                            logger.warn { "[Timeline] Rate limit: API calls seem to be frequent than expected. Sleep 5 secs." }
                            TimeUnit.SECONDS.sleep(5)
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "[Timeline] An error occurred while getting timeline for @${account.sn}." }
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
            val friends = client.friend.listIds().complete().untilLast().flatMap { it.result.ids }
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
