package jp.nephy.tweetstorm

import com.google.gson.JsonObject
import io.ktor.util.cio.ChannelWriteException
import jp.nephy.jsonkt.JsonKt
import jp.nephy.jsonkt.JsonModel
import jp.nephy.jsonkt.jsonObject
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.task.*
import mu.KotlinLogging
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TaskManager(val account: Config.Account, initialStream: AuthenticatedStream) {
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
            } catch (e: Exception) {
                // if list does not contain me
                it.add(UserTimeline(this))
                it.add(MentionTimeline(this))
            }
        }

        // it.add(DirectMessage(this))
        // it.add(Activity(this))
        // it.add(Delete(this))
    }
    private val targetedTasks: List<TargetedFetchTask<*>> = mutableListOf<TargetedFetchTask<*>>().also {
        if (account.enableFriends) {
            it.add(Friends(this))
        }
    }

    @Synchronized
    fun bind(stream: AuthenticatedStream) {
        if (stream !in streams) {
            streams.add(stream)
            startTargetedTasks(stream)
            logger.info { "A Stream has been bind to ${account.displayName}." }
        }
    }

    @Synchronized
    fun unbind(stream: AuthenticatedStream) {
        if (stream in streams) {
            streams.remove(stream)
            logger.info { "A Stream has been unbind from ${account.displayName}." }
        }
    }

    fun emit(target: AuthenticatedStream, content: String) {
        try {
            target.send(content)
        } catch (e: ChannelWriteException) {
            unbind(target)
        } catch (e: Exception) {
            logger.error(e) { "A Stream failed sending payload. (${account.displayName})" }
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
        executor.execute {
            for (it in streams) {
                executor.execute { emit(it, content) }
            }
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
