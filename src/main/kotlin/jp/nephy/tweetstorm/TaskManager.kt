package jp.nephy.tweetstorm

import com.google.gson.JsonObject
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.jsonObject
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.penicillin.core.TwitterErrorMessage
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.task.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.sync.Mutex
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
    val twitter = PenicillinClient {
        account {
            application(account.ck, account.cs)
            token(account.at, account.ats)
        }
    }

    private val streams = CopyOnWriteArraySet<AuthenticatedStream>().also {
        it += initialStream
    }

    private val tasks = mutableListOf<RunnableTask>().also {
        if (account.listId != null) {
            it += ListTimeline(this)

            try {
                twitter.list.member(listId = account.listId, userId = account.user.id).complete()
            } catch (e: PenicillinException) {
                if (e.error == TwitterErrorMessage.UserNotInThisList) {
                    it += UserTimeline(this)
                    it += MentionTimeline(this)
                }
            }
        } else {
            it += HomeTimeline(this)
        }

        if (account.enableDirectMessage) {
            it += DirectMessage(this)
        }
        if (account.filterStreamTracks.isNotEmpty() || account.filterStreamFollows.isNotEmpty()) {
            it += FilterStream(this)
        }
        if (account.enableSampleStream) {
            it += SampleStream(this)
        }
        if (account.enableActivity && account.twitterForiPhoneAccessToken != null && account.twitterForiPhoneAccessTokenSecret != null) {
            it += Activity(this)
        }
        it += Heartbeat(this)
    }
    private val targetedTasks = mutableListOf<TargetedFetchTask>().also {
        if (account.enableFriends) {
            it += Friends(this)
        }
    }
    private val regularTasks = mutableListOf<RegularTask>().also {
        if (account.syncListFollowing && account.listId != null) {
            it += SyncList(this)
        }
    }

    fun anyClients(): Boolean {
        return streams.isNotEmpty()
    }

    fun register(stream: AuthenticatedStream) {
        if (streams.add(stream)) {
            startTargetedTasks(stream)
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
        while (stream.handler.isAlive) {
            delay(1, TimeUnit.SECONDS)
        }

        unregister(stream)
    }

    suspend fun emit(target: AuthenticatedStream, content: String) {
        try {
            target.handler.emit(content)
            logger.trace { "Payload = $content" }
        } catch (e: IOException) {
            unregister(target)
        } catch (e: Exception) {
            logger.error(e) { "A Stream failed sending payload. ($content)" }
        }
    }
    suspend fun emit(target: AuthenticatedStream, vararg pairs: Pair<String, Any?>) {
        emit(target, jsonObject(*pairs))
    }
    suspend fun emit(target: AuthenticatedStream, json: JsonObject) {
        emit(target, json.toJsonString())
    }
    suspend fun emit(target: AuthenticatedStream, payload: JsonModel) {
        emit(target, payload.json)
    }
    suspend fun emit(content: String) {
        for (stream in streams) {
            launch(parent = stream.job) {
                emit(stream, content)
            }
        }
    }
    suspend fun emit(vararg pairs: Pair<String, Any?>) {
        emit(jsonObject(*pairs))
    }
    suspend fun emit(json: JsonObject) {
        emit(json.toJsonString())
    }
    suspend fun emit(payload: JsonModel) {
        emit(payload.json)
    }

    suspend fun heartbeat() {
        for (stream in streams) {
            launch(parent = stream.job) {
                try {
                    stream.handler.heartbeat()
                } catch (e: IOException) {
                    unregister(stream)
                } catch (e: Exception) {
                    logger.error(e) { "A Stream failed sending heartbeat." }
                }
            }
        }
    }

    fun start(target: AuthenticatedStream) {
        startTasks()
        startTargetedTasks(target)
        startRegularTasks()
    }

    private fun startTasks() {
        for (task in tasks) {
            launch(parent = masterJob) {
                task.logger.debug { "RunnableTask: ${task.javaClass.simpleName} started." }

                while (isActive) {
                    try {
                        task.run()
                    } catch (e: CancellationException) {
                        task.logger.trace(e) { "RunnableTask: ${task.javaClass.simpleName} was cancelled." }
                        break
                    } catch (e: Exception) {
                        task.logger.error(e) { "An error occurred while task." }
                    }
                }

                withContext(NonCancellable) {
                    task.logger.debug { "RunnableTask: ${task.javaClass.simpleName} will terminate." }
                    task.close()
                }
            }
        }
    }

    private fun startTargetedTasks(target: AuthenticatedStream) {
        for (task in targetedTasks) {
            launch(parent = masterJob) {
                task.logger.debug { "TargetedTask: ${task.javaClass.simpleName} started." }

                try {
                    task.run(target)
                    task.logger.debug { "TargetedTask: ${task.javaClass.simpleName} finished." }
                } catch (e: CancellationException) {
                    task.logger.trace(e) { "TargetedTask: ${task.javaClass.simpleName} was cancelled." }
                } catch (e: Exception) {
                    task.logger.error(e) { "An error occurred while targeted task." }
                } finally {
                    withContext(NonCancellable) {
                        task.close()
                    }
                }
            }
        }
    }

    private fun startRegularTasks() {
        for (task in regularTasks) {
            launch(parent = masterJob) {
                while (isActive) {
                    task.logger.debug { "RegularTask: ${task.javaClass.simpleName} started." }

                    try {
                        task.run()
                        task.logger.debug { "RegularTask: ${task.javaClass.simpleName} finished." }
                        delay(task.interval, task.unit)
                    } catch (e: CancellationException) {
                        task.logger.trace(e) { "RegularTask: ${task.javaClass.simpleName} was cancelled." }
                        break
                    } catch (e: Exception) {
                        task.logger.error(e) { "An error occurred while regular task." }
                        delay(task.interval, task.unit)
                    }
                }

                withContext(NonCancellable) {
                    task.logger.debug { "RegularTask: ${task.javaClass.simpleName} will terminate." }
                    task.close()
                }
            }
        }
    }

    override fun close() {
        runBlocking(CommonPool) {
            masterJob.children.filter { it.isActive }.forEach {
                it.cancelAndJoin()
            }
            masterJob.cancelAndJoin()
        }

        twitter.close()
    }
}
