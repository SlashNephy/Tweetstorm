package jp.nephy.tweetstorm

import com.google.gson.JsonObject
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.jsonObject
import jp.nephy.jsonkt.toJsonString
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.PenicillinException
import jp.nephy.tweetstorm.builder.CustomStatusBuilder
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.task.*
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TaskManager(initialStream: AuthenticatedStream): Closeable {
    val account = initialStream.account
    private val logger by lazy { logger("Tweetstorm.TaskManager (${account.displayName})") }
    private val executor = Executors.newCachedThreadPool()
    val twitter = PenicillinClient {
        account {
            application(account.ck, account.cs)
            token(account.at, account.ats)
        }
    }
    private val streams = CopyOnWriteArrayList<AuthenticatedStream>().also {
        it.add(initialStream)
    }
    private val tasks: List<FetchTask> = mutableListOf<FetchTask>().also {
        if (account.listId != null) {
            it.add(ListTimeline(this))

            try {
                twitter.list.member(listId = account.listId, userId = account.id).complete()
            } catch (e: PenicillinException) {
                // if list does not contain me
                it.add(UserTimeline(this))
                it.add(MentionTimeline(this))
            }
        } else {
            it.add(HomeTimeline(this))
        }

        // it.add(DirectMessage(this))
        // it.add(Activity(this))
        // it.add(Delete(this))
        it.add(Heartbeat(this))
    }
    private val targetedTasks: List<TargetedFetchTask> = mutableListOf<TargetedFetchTask>().also {
        if (account.enableFriends) {
            it.add(Friends(this))
        }
    }
    private val regularTasks: List<RegularTask> = mutableListOf<RegularTask>().also {
        if (account.syncListFollowing && account.listId != null) {
            it.add(SyncList(this))
        }
    }
    val shouldTerminate: Boolean
        get() = streams.isEmpty()

    @Synchronized
    fun register(stream: AuthenticatedStream) {
        if (stream !in streams && streams.add(stream)) {
            startTargetedTasks(stream)
            logger.debug { "A Stream has been registered to ${account.displayName}." }
        }
    }

    @Synchronized
    fun unregister(stream: AuthenticatedStream) {
        if (stream in streams && streams.remove(stream)) {
            logger.debug { "A Stream has been unregistered from ${account.displayName}." }
        }
    }

    fun wait(stream: AuthenticatedStream) {
        while (stream.isAlive) {
            TimeUnit.SECONDS.sleep(3)
        }
        unregister(stream)
    }

    fun emit(target: AuthenticatedStream, content: String) {
        try {
            target.send(content)
        } catch (e: IOException) {
            unregister(target)
        } catch (e: Exception) {
            logger.error(e) { "A Stream failed sending payload. ($content)" }
        }
    }
    fun emit(target: AuthenticatedStream, vararg pairs: Pair<String, Any?>) {
        emit(target, jsonObject(*pairs))
    }
    fun emit(target: AuthenticatedStream, json: JsonObject) {
        emit(target, json.toJsonString())
    }
    fun emit(target: AuthenticatedStream, payload: JsonModel) {
        emit(target, payload.json)
    }
    fun emit(target: AuthenticatedStream, builder: CustomStatusBuilder.() -> Unit) {
        emit(target, CustomStatusBuilder.new(builder))
    }
    fun emit(content: String) {
        for (it in streams) {
            emit(it, content)
        }
    }
    fun emit(vararg pairs: Pair<String, Any?>) {
        emit(jsonObject(*pairs))
    }
    fun emit(json: JsonObject) {
        emit(json.toJsonString())
    }
    fun emit(payload: JsonModel) {
        emit(payload.json)
    }
    fun emit(builder: CustomStatusBuilder.() -> Unit) {
        emit(CustomStatusBuilder.new(builder))
    }

    fun heartbeat() {
        for (it in streams) {
            try {
                it.heartbeat()
            } catch (e: IOException) {
                unregister(it)
            } catch (e: Exception) {
                logger.error(e) { "A Stream failed sending heartbeat." }
            }
        }
    }

    fun start(target: AuthenticatedStream) {
        startTasks()
        startTargetedTasks(target)
        startRegularTasks()
    }
    private fun startTasks() {
        tasks.forEach {
            executor.execute {
                it.logger.debug { "FetchTask: ${it.javaClass.simpleName} started." }
                while (!shouldTerminate) {
                    try {
                        it.run()
                    } catch (e: InterruptedException) {
                        break
                    } catch (e: Exception) {
                        it.logger.error(e) { "An error occurred while fetching." }
                    }
                }
                it.logger.debug { "FetchTask: ${it.javaClass.simpleName} will terminate." }
            }
        }
    }
    private fun startTargetedTasks(target: AuthenticatedStream) {
        targetedTasks.forEach {
            executor.execute {
                it.logger.debug { "TargetedFetchTask: ${it.javaClass.simpleName} started." }
                try {
                    it.run(target)
                    it.logger.debug { "TargetedFetchTask: ${it.javaClass.simpleName} finished." }
                } catch (e: InterruptedException) {
                } catch (e: Exception) {
                    it.logger.error(e) { "An error occurred while targeted fetching." }
                }
            }
        }
    }
    private fun startRegularTasks() {
        regularTasks.forEach {
            executor.execute {
                while (!shouldTerminate) {
                    it.logger.debug { "RegularTask: ${it.javaClass.simpleName} started." }
                    try {
                        it.run()
                        it.logger.debug { "RegularTask: ${it.javaClass.simpleName} finished." }
                    } catch (e: InterruptedException) {
                        break
                    } catch (e: Exception) {
                        it.logger.error(e) { "An error occurred while regular task." }
                    } finally {
                        try {
                            it.unit.sleep(it.interval)
                        } catch (e: InterruptedException) {
                        }
                    }
                }
                it.logger.debug { "RegularTask: ${it.javaClass.simpleName} will terminate." }
            }
        }
    }

    override fun close() {
        executor.shutdownNow()
    }
}
