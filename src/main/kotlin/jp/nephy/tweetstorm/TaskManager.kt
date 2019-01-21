package jp.nephy.tweetstorm

import jp.nephy.penicillin.core.exceptions.PenicillinException
import jp.nephy.penicillin.endpoints.lists
import jp.nephy.penicillin.extensions.complete
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.task.ProduceTask
import jp.nephy.tweetstorm.task.RegularTask
import jp.nephy.tweetstorm.task.producer.*
import jp.nephy.tweetstorm.task.regular.SyncList
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArraySet

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
    private val twitterClient = account.twitter

    fun anyClients(): Boolean {
        return streams.isNotEmpty()
    }

    private val tasks = object {
        val produce = mutableListOf<ProduceTask<*>>().also {
            if (account.listId != null) {
                it += ListTimeline(account, twitterClient)

                val listContainsSelf = try {
                    twitterClient.lists.member(listId = account.listId!!, userId = account.user.id).complete()
                    true
                } catch (e: PenicillinException) {
                    false
                }

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
        }

        val regular = mutableListOf<RegularTask>().also {
            if (account.syncList.enabled && account.listId != null) {
                it += SyncList(account, twitterClient)
            }
        }
    }

    @ExperimentalCoroutinesApi
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
            delay(1000)
        }

        unregister(stream)
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    suspend fun start(target: AuthenticatedStream) {
        target.startTargetedTasks()

        for (task in tasks.produce) {
            GlobalScope.launch(dispatcher + masterJob) {
                task.logger.debug { "ProduceTask: ${task.javaClass.simpleName} started." }

                while (isActive) {
                    task.channel(coroutineContext, masterJob).consumeEach { data ->
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
        }

        for (task in tasks.regular) {
            GlobalScope.launch(dispatcher + masterJob) {
                while (isActive) {
                    task.logger.debug { "RegularTask: ${task.javaClass.simpleName} started." }

                    try {
                        task.run()
                        task.logger.trace { "RegularTask: ${task.javaClass.simpleName} finished successfully." }
                        delay(task.unit.toMillis(task.interval))
                    } catch (e: CancellationException) {
                        task.logger.debug { "RegularTask: ${task.javaClass.simpleName} will terminate." }
                        break
                    } catch (e: Exception) {
                        task.logger.error(e) { "An error occurred while regular task." }
                        delay(task.unit.toMillis(task.interval))
                    }
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    private suspend fun AuthenticatedStream.startTargetedTasks() {
        GlobalScope.launch(dispatcher + masterJob) {
            select {
                if (account.enableFriends) {
                    val task = Friends(this@startTargetedTasks)
                    launch(dispatcher + job) {
                        task.channel(coroutineContext, masterJob).onReceive { data ->
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
        twitterClient.close()

        runBlocking(Dispatchers.Default) {
            masterJob.cancelChildren()
            masterJob.cancelAndJoin()
        }
    }
}
