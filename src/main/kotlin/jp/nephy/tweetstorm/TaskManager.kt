package jp.nephy.tweetstorm

import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.TwitterErrorMessage
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
import java.io.IOException
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

                try {
                    account.twitter.list.member(listId = account.listId, userId = account.user.id).complete()
                } catch (e: PenicillinException) {
                    if (e.error == TwitterErrorMessage.UserNotInThisList) {
                        it += UserTimeline(account)
                        it += MentionTimeline(account)
                    }
                }
            } else {
                it += HomeTimeline(account)
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

        launch(parent = masterJob) {
            select {
                for (task in tasks.produce) {
                    task.channel.onReceiveOrNull { data ->
                        if (data == null) {
                            task.logger.trace { "ProduceTask: ${task.javaClass.simpleName} channel closed." }
                            return@onReceiveOrNull
                        }

                        for (stream in streams) {
                            try {
                                data.emit(stream.handler)
                                task.logger.trace { "ProduceTask: ${task.javaClass.simpleName} emitted." }
                            } catch (e: IOException) {
                                task.logger.trace { "ProduceTask: ${task.javaClass.simpleName} finished." }
                                unregister(stream)
                            }
                        }
                    }

                    task.logger.debug { "ProduceTask: ${task.javaClass.simpleName} has been registered." }
                }
            }
        }

        for (task in tasks.regular) {
            launch(parent = masterJob) {
                while (isActive) {
                    task.logger.debug { "RegularTask: ${task.javaClass.simpleName} started." }

                    try {
                        task.run()
                        task.logger.debug { "RegularTask: ${task.javaClass.simpleName} finished." }
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
                    task.channel.onReceiveOrNull { data ->
                        if (data == null) {
                            task.logger.trace { "TargetedProduceTask: ${task.javaClass.simpleName} channel closed." }
                            return@onReceiveOrNull
                        }

                        try {
                            data.emit(handler)
                        } catch (e: IOException) {
                            task.logger.trace { "TargetedProduceTask: ${task.javaClass.simpleName} finished." }
                            unregister(this@startTargetedTasks)
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
