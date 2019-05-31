package jp.nephy.tweetstorm

import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.extensions.complete
import jp.nephy.penicillin.extensions.endpoints.hasMember
import jp.nephy.tweetstorm.producer.Producer
import jp.nephy.tweetstorm.producer.impl.*
import jp.nephy.tweetstorm.session.AuthenticatedStream
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.selects.select
import kotlinx.io.core.Closeable

class StreamManager(initialStream: AuthenticatedStream): Closeable {
    companion object {
        val instances = ConcurrentMutableList<StreamManager>()

        fun register(stream: AuthenticatedStream): StreamManager {
            return instances.find {
                it.account.user.id == stream.account.user.id
            }?.also {
                it.register(stream)
            } ?: StreamManager(stream).also {
                instances += it
                it.start(stream)
            }
        }
    }

    val account = initialStream.account
    private val twitterClient = account.twitter
    private val masterJob = Job()

    private val logger = logger("Tweetstorm.StreamManager (@${account.user.screenName})")

    private val streams = ConcurrentMutableList<AuthenticatedStream>().also {
        it += initialStream
    }

    private val producers = ConcurrentMutableList<Producer<*>>().also {
        if (account.listId != null) {
            it += ListTimeline(account, twitterClient)

            val listContainsSelf = twitterClient.lists.hasMember(listId = account.listId!!, userId = account.user.id).complete()

            if (listContainsSelf && account.syncList.enabled) {
                it += UserTimeline(account, twitterClient) { status ->
                    status.retweetedStatus == null && status.inReplyToUserId != null && status.inReplyToUserId!! !in account.friends
                }
                it += MentionTimeline(account, twitterClient) { status ->
                    status.user.id !in account.friends
                }
            } else {
                it += UserTimeline(account, twitterClient)
                it += MentionTimeline(account, twitterClient)
            }
        } else {
            it += HomeTimeline(account, twitterClient)
            it += UserTimeline(account, twitterClient) { status ->
                status.retweetedStatus == null && status.inReplyToUserId != null && status.inReplyToUserId!! !in account.friends
            }
            it += MentionTimeline(account, twitterClient) { status ->
                status.user.id !in account.friends
            }
        }

        if (account.enableDirectMessage) {
            it += DirectMessage(account, twitterClient)
        }

        if (account.filterStream.tracks.isNotEmpty() || account.filterStream.follows.isNotEmpty()) {
            it += FilterStream(account, twitterClient)
        }

        if (account.enableSampleStream) {
            it += SampleStream(account, twitterClient)
        }

        if (account.enableActivity && account.t4i.at != null && account.t4i.ats != null) {
            it += Activity(account)
        }

        it += Heartbeat(account)

        if (account.syncList.enabled && account.listId != null) {
            it += SyncList(account, twitterClient)
        }
    }

    @ExperimentalCoroutinesApi
    fun register(stream: AuthenticatedStream) {
        if (streams.add(stream)) {
            stream.startTargetedTasks()
            logger.debug { "A Stream has been registered to @${account.user.screenName}." }
        }
    }

    private fun unregister(stream: AuthenticatedStream) {
        if (streams.remove(stream)) {
            stream.close()
            logger.debug { "A Stream has been unregistered from @${account.user.screenName}." }
        }
    }

    suspend fun wait(stream: AuthenticatedStream) {
        while (stream.writer.isAlive) {
            delay(1000)
        }

        unregister(stream)

        if (streams.isEmpty()) {
            if (instances.removeIf { it.account.user.id == account.user.id }) {
                logger.debug { "Task Manager: @${account.user.screenName} will terminate." }
                close()
            }
        }
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    fun start(target: AuthenticatedStream) {
        target.startTargetedTasks()

        for (producer in producers) {
            Tweetstorm.launch(masterJob) {
                while (isActive) {
                    producer.start(masterJob).consumeEach { data ->
                        for (stream in streams) {
                            if (data.emit(stream.writer)) {
                                producer.logger.trace { "データを送信しました。" }
                            } else {
                                producer.logger.trace { "データの送信に失敗しました。" }
                                unregister(stream)
                            }
                        }
                    }
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    private fun AuthenticatedStream.startTargetedTasks() {
        Tweetstorm.launch(masterJob) {
            select<Unit> {
                if (account.enableFriends) {
                    val producer = Friends(this@startTargetedTasks)

                    launch(job) {
                        val channel = producer.start(masterJob)

                        channel.onReceive { data ->
                            if (data.emit(writer)) {
                                producer.logger.trace { "データを送信しました。" }
                            } else {
                                producer.logger.trace { "データの送信に失敗しました。" }
                                unregister(this@startTargetedTasks)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun close() {
        twitterClient.close()
        masterJob.cancel()
    }
}
