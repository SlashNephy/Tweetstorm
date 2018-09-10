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
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

class TaskManager(initialStream: AuthenticatedStream): Closeable {
    val account = initialStream.account
    private val logger by lazy { logger("Tweetstorm.TaskManager (@${account.user.screenName})") }
    val twitter = PenicillinClient {
        account {
            application(account.ck, account.cs)
            token(account.at, account.ats)
        }
    }
    private val streams = CopyOnWriteArraySet<AuthenticatedStream>().also {
        it.add(initialStream)
    }
    private val tasks = mutableListOf<RunnableTask>().also {
        if (account.listId != null) {
            it.add(ListTimeline(this))

            try {
                twitter.list.member(listId = account.listId, userId = account.user.id).complete()
            } catch (e: PenicillinException) {
                if (e.error == TwitterErrorMessage.UserNotInThisList) {
                    it.add(UserTimeline(this))
                    it.add(MentionTimeline(this))
                }
            }
        } else {
            it.add(HomeTimeline(this))
        }

        if (account.enableDirectMessage) {
            it.add(DirectMessage(this))
        }
        if (account.filterStreamTracks.isNotEmpty() || account.filterStreamFollows.isNotEmpty()) {
            it.add(FilterStream(this))
        }
        if (account.enableSampleStream) {
            it.add(SampleStream(this))
        }
        if (account.enableActivity && account.twitterForiPhoneAccessToken != null && account.twitterForiPhoneAccessTokenSecret != null) {
            it.add(Activity(this))
        }
        it.add(Heartbeat(this))
    }
    private val targetedTasks = mutableListOf<TargetedFetchTask>().also {
        if (account.enableFriends) {
            it.add(Friends(this))
        }
    }
    private val regularTasks = mutableListOf<RegularTask>().also {
        if (account.syncListFollowing && account.listId != null) {
            it.add(SyncList(this))
        }
    }
    private val jobs = CopyOnWriteArraySet<Job>()

    fun anyClients(): Boolean {
        return streams.isNotEmpty()
    }

    fun register(stream: AuthenticatedStream) {
        if (synchronized(stream) { streams.add(stream) }) {
            startTargetedTasks(stream)
            logger.debug { "A Stream has been registered to @${account.user.screenName}." }
        }
    }

    private fun unregister(stream: AuthenticatedStream) {
        if (synchronized(stream) { streams.remove(stream) }) {
            runBlocking {
                jobs.filter { it.isActive }.forEach {
                    it.cancelAndJoin()
                }
            }
            logger.debug { "A Stream has been unregistered from @${account.user.screenName}." }
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
            logger.trace { "Payload = $content" }
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
            jobs += launch {
                it.logger.debug { "RunnableTask: ${it.javaClass.simpleName} started." }

                while (isActive) {
                    try {
                        it.run()
                    } catch (e: CancellationException) {
                        break
                    } catch (e: Exception) {
                        it.logger.error(e) { "An error occurred while task." }
                    }
                }

                withContext(NonCancellable) {
                    it.close()
                    it.logger.debug { "RunnableTask: ${it.javaClass.simpleName} will terminate." }
                }
            }
        }
    }
    private fun startTargetedTasks(target: AuthenticatedStream) {
        targetedTasks.forEach {
            jobs += launch {
                it.logger.debug { "TargetedFetchTask: ${it.javaClass.simpleName} started." }
                try {
                    it.run(target)
                    it.logger.debug { "TargetedFetchTask: ${it.javaClass.simpleName} finished." }
                } catch (e: CancellationException) {
                } catch (e: Exception) {
                    it.logger.error(e) { "An error occurred while targeted task." }
                } finally {
                    withContext(NonCancellable) {
                        it.close()
                    }
                }
            }
        }
    }
    private fun startRegularTasks() {
        regularTasks.forEach {
            jobs += launch {
                while (isActive) {
                    it.logger.debug { "RegularTask: ${it.javaClass.simpleName} started." }
                    try {
                        it.run()
                        it.logger.debug { "RegularTask: ${it.javaClass.simpleName} finished." }
                        it.unit.sleep(it.interval)
                    } catch (e: CancellationException) {
                        break
                    } catch (e: Exception) {
                        it.logger.error(e) { "An error occurred while regular task." }
                        it.unit.sleep(it.interval)
                    }
                }

                withContext(NonCancellable) {
                    it.close()
                    it.logger.debug { "RegularTask: ${it.javaClass.simpleName} will terminate." }
                }
            }
        }
    }

    override fun close() {
        twitter.close()
    }
}
