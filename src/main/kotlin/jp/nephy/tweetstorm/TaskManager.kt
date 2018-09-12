package jp.nephy.tweetstorm

import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.RegularTask
import jp.nephy.tweetstorm.task.producer.*
import jp.nephy.tweetstorm.task.regular.SyncList
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.selects.select
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

class TaskManager(initialStream: AuthenticatedStream): Closeable {
    companion object {
        val instances = CopyOnWriteArraySet<TaskManager>()
        val mutex = Mutex()
    }

    private val logger = logger("Tweetstorm.TaskManager (@${initialStream.account.user.screenName})")
    private val masterJob = Job()

    val account = initialStream.account

    private val streams = CopyOnWriteArraySet<AuthenticatedStream>().also {
        it += initialStream
    }
    private val streamsMutex = Mutex()

    fun anyClients(): Boolean {
        return streams.isNotEmpty()
    }

    private val tasks = object {
        val produce = mutableListOf<ProduceTask<*>>().also {
            if (account.listId != null) {
                it += ListTimeline(account)

                val listContainsSelf = try {
                    account.twitter.list.member(listId = account.listId, userId = account.user.id).complete()
                    true
                } catch (e: PenicillinException) {
                    false
                }

                if (listContainsSelf && account.syncListFollowing) {
                    it += UserTimeline(account) { status ->
                        status.inReplyToUserId !in account.friends
                    }
                    it += MentionTimeline(account) { status ->
                        status.user.id !in account.friends
                    }
                } else {
                    it += UserTimeline(account)
                    it += MentionTimeline(account)
                }
            } else {
                it += HomeTimeline(account)
                it += UserTimeline(account) { status ->
                    status.inReplyToUserId !in account.friends
                }
                it += MentionTimeline(account) { status ->
                    status.user.id !in account.friends
                }
            }

            if (account.enableDirectMessage) {
                it += DirectMessage(account)
            }

            if (account.filterStreamTracks.isNotEmpty() || account.filterStreamFollows.isNotEmpty()) {
                it += FilterStream(account)
            }

            if (account.enableSampleStream) {
                it += SampleStream(account)
            }

            if (account.enableActivity && account.twitterForiPhoneAccessToken != null && account.twitterForiPhoneAccessTokenSecret != null) {
                it += Activity(account)
            }

            it += Heartbeat(account)
        }

        val regular = mutableListOf<RegularTask>().also {
            if (account.syncListFollowing && account.listId != null) {
                it += SyncList(account)
            }
        }
    }

    suspend fun register(stream: AuthenticatedStream) {
        if (streamsMutex.withLock { streams.add(stream) }) {
            stream.startTargetedTasks()
            logger.debug { "A Stream has been registered to @${account.user.screenName}." }
        }
    }

    private suspend fun unregister(stream: AuthenticatedStream) {
        if (streamsMutex.withLock { streams.remove(stream) }) {
            stream.close()
            logger.debug { "A Stream has been unregistered from @${account.user.screenName}." }
        }
    }

    suspend fun wait(stream: AuthenticatedStream) {
        while (stream.handler.isAlive) {
            delay(1, TimeUnit.SECONDS)
        }

        unregister(stream)
    }

    suspend fun start(target: AuthenticatedStream) {
        target.startTargetedTasks()

        for (task in tasks.produce) {
            launch(parent = masterJob) {
                task.logger.debug { "ProduceTask: ${task.javaClass.simpleName} started." }

                while (isActive) {
                    val channel = task.channel(kotlin.coroutines.experimental.coroutineContext, masterJob)
                    val data = channel.receive()
                    for (stream in streams) {
                        if (data.emit(stream.handler)) {
                            task.logger.trace { "${data.javaClass.simpleName} (${task.javaClass.simpleName}) emitted successfully." }
                        } else {
                            task.logger.trace { "${data.javaClass.simpleName} (${task.javaClass.simpleName}) failed to deliver." }
                            unregister(stream)
                        }
                    }
                }
            }
        }

        for (task in tasks.regular) {
            launch(parent = masterJob) {
                while (isActive) {
                    task.logger.debug { "RegularTask: ${task.javaClass.simpleName} started." }

                    try {
                        task.run()
                        task.logger.trace { "RegularTask: ${task.javaClass.simpleName} finished successfully." }
                        delay(task.interval, task.unit)
                    } catch (e: CancellationException) {
                        task.logger.debug { "RegularTask: ${task.javaClass.simpleName} will terminate." }
                        break
                    } catch (e: Exception) {
                        task.logger.error(e) { "An error occurred while regular task." }
                        delay(task.interval, task.unit)
                    }
                }
            }
        }
    }

    private suspend fun AuthenticatedStream.startTargetedTasks() {
        launch(parent = masterJob) {
            select {
                if (account.enableFriends) {
                    val task = Friends(this@startTargetedTasks)
                    launch(parent = job) {
                        task.channel(kotlin.coroutines.experimental.coroutineContext, masterJob).onReceive { data ->
                            if (data.emit(handler)) {
                                task.logger.trace { "${data.javaClass.simpleName} (${task.javaClass.simpleName}) emitted successfully." }
                            } else {
                                task.logger.debug { "${data.javaClass.simpleName} (${task.javaClass.simpleName}) failed to deliver." }
                                unregister(this@startTargetedTasks)
                            }
                        }
                    }

                    task.logger.debug { "TargetedProduceTask: ${task.javaClass.simpleName} started." }
                }
            }
        }
    }

    override fun close() {
        runBlocking(CommonPool) {
            masterJob.cancelChildren()
            masterJob.cancelAndJoin()
        }
    }
}
