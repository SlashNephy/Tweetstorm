package jp.nephy.tweetstorm

import com.google.gson.JsonObject
import io.ktor.util.cio.ChannelWriteException
import jp.nephy.jsonkt.JsonKt
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.jsonObject
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.PenicillinException
import jp.nephy.penicillin.TwitterApiError
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.task.*
import mu.KotlinLogging
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TaskManager(initialStream: AuthenticatedStream) {
    val account = initialStream.account
    private val logger = KotlinLogging.logger("Tweetstorm.TaskManager (${account.displayName})")
    private val executor = Executors.newCachedThreadPool()
    val twitter = PenicillinClient.build {
        application(account.ck, account.cs)
        token(account.at, account.ats)
    }
    val streams = CopyOnWriteArrayList<AuthenticatedStream>().also {
        it.add(initialStream)
    }
    private val tasks: List<FetchTask<*>> = mutableListOf<FetchTask<*>>().also {
        if (account.listId != null) {
            it.add(ListTimeline(this))
        } else {
            it.add(HomeTimeline(this))
        }

        if (account.listId != null) {
            try {
                twitter.list.member(listId = account.listId, userId = account.id).complete()
            } catch (e: PenicillinException) {
                // if list does not contain me
                it.add(UserTimeline(this))
                it.add(MentionTimeline(this))
            }
        }

        // it.add(DirectMessage(this))
        // it.add(Activity(this))
        // it.add(Delete(this))
        it.add(Heartbeat(this))
    }
    private val targetedTasks: List<TargetedFetchTask<*>> = mutableListOf<TargetedFetchTask<*>>().also {
        if (account.enableFriends) {
            it.add(Friends(this))
        }
    }

    @Synchronized
    fun register(stream: AuthenticatedStream) {
        if (stream !in streams && streams.add(stream)) {
            startTargetedTasks(stream)
            logger.info { "A Stream has been registered to ${account.displayName}." }
        }
    }

    @Synchronized
    fun unregister(stream: AuthenticatedStream) {
        if (stream in streams && streams.remove(stream)) {
            logger.info { "A Stream has been unregistered from ${account.displayName}." }
        }
    }

    fun wait(stream: AuthenticatedStream) {
        while (stream.isAlive) {
            TimeUnit.SECONDS.sleep(3)
        }
    }

    fun emit(target: AuthenticatedStream, content: String) {
        try {
            target.send(content)
        } catch (e: IOException) {
            unregister(target)
        } catch (e: Exception) {
            logger.error(e) { "A Stream failed sending payload." }
        }
    }
    fun emit(target: AuthenticatedStream, vararg pairs: Pair<String, Any?>) {
        emit(target, jsonObject(*pairs))
    }
    fun emit(target: AuthenticatedStream, json: JsonObject) {
        emit(target, JsonKt.toJsonString(json))
    }
    fun emit(target: AuthenticatedStream, payload: JsonModel) {
        emit(target, payload.json)
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
        emit(JsonKt.toJsonString(json))
    }
    fun emit(payload: JsonModel) {
        emit(payload.json)
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

    fun startTasks() {
        tasks.forEach {
            executor.execute {
                logger.debug { "FetchTask: ${it.javaClass.simpleName} started." }
                while (true) {
                    it.fetch()
                    TimeUnit.SECONDS.sleep(5)
                }
            }
        }
    }

    fun startTargetedTasks(target: AuthenticatedStream) {
        targetedTasks.forEach {
            executor.execute {
                logger.debug { "TargetedFetchTask: ${it.javaClass.simpleName} started." }
                it.fetch(target)
            }
        }
    }
}
