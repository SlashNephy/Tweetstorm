package jp.nephy.tweetstorm

import ch.qos.logback.classic.Level
import io.ktor.client.engine.apache.Apache
import jp.nephy.jsonkt.ImmutableJsonObject
import jp.nephy.jsonkt.delegation.*
import jp.nephy.jsonkt.nullableString
import jp.nephy.jsonkt.parse
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.allIds
import kotlinx.io.core.use
import java.nio.file.Path
import java.nio.file.Paths

private val logger = jp.nephy.tweetstorm.logger("Tweetstorm.Config")

data class Config(override val json: ImmutableJsonObject): JsonModel {
    companion object {
        private val defaultConfigPath = Paths.get("config.json")

        fun load(configPath: Path?): Config {
            return try {
                (configPath ?: defaultConfigPath).parse()
            } catch (e: Exception) {
                throw IllegalStateException("config.json is invalid.")
            }
        }
    }

    val wui by lazy { WebUI(json) }
    data class WebUI(override val json: ImmutableJsonObject): JsonModel {
        val host by string { "127.0.0.1" }
        val port by int { 8080 }
        val maxConnections by nullableInt("max_connections")
    }

    val app by lazy { App(json) }
    data class App(override val json: ImmutableJsonObject): JsonModel {
        val skipAuth by boolean("skip_auth") { false }
        val apiTimeout by long("api_timeout") { 3000 }
        val parallelism by int("parallelism") { maxOf(1, Runtime.getRuntime().availableProcessors() / 2) }
    }

    val logLevel by lambda("log_level") { Level.toLevel(it.nullableString, Level.INFO)!! }

    val accounts by modelList<Account>()
    data class Account(override val json: ImmutableJsonObject): JsonModel {
        val ck by string
        val cs by string
        val at by string
        val ats by string
        val listId by nullableLong("list_id")
        val token by nullableString

        val enableDirectMessage by boolean("enable_direct_message") { true }
        val enableActivity by boolean("enable_activity") { false }
        val enableFriends by boolean("enable_friends") { true }
        val enableSampleStream by boolean("enable_sample_stream") { false }

        val filterStream by lazy { FilterStream(json) }
        data class FilterStream(override val json: ImmutableJsonObject): JsonModel {
            val tracks by stringList("filter_stream_tracks")
            val follows by longList("filter_stream_follows")
        }

        val syncList by lazy { SyncList(json) }
        data class SyncList(override val json: ImmutableJsonObject): JsonModel {
            val enabled by boolean("sync_list_following") { false }
            val includeSelf by boolean("sync_list_include_self") { true }
        }

        val t4i by lazy { T4iCredentials(json) }
        data class T4iCredentials(override val json: ImmutableJsonObject): JsonModel {
            val at by nullableString("t4i_at")
            val ats by nullableString("t4i_ats")
        }

        val refresh by lazy { RefreshTime(json) }
        data class RefreshTime(override val json: ImmutableJsonObject): JsonModel {
            private val listTimelineSec by nullableLong("list_timeline_refresh_sec")
            private val userTimelineSec by nullableLong("user_timeline_refresh_sec")
            private val mentionTimelineSec by nullableLong("mention_timeline_refresh_sec")
            private val homeTimelineSec by nullableLong("home_timeline_refresh_sec")
            private val directMessageSec by nullableLong("direct_message_refresh_sec")
            private val activitySec by nullableLong("activity_refresh_sec")

            init {
                if (listTimelineSec != null || userTimelineSec != null || mentionTimelineSec != null || homeTimelineSec != null || directMessageSec != null || activitySec != null) {
                    logger.warn { "`*_refresh_sec` are deprecated in config.json. Please use `*_refresh` instead." }
                }
            }

            private val listTimelineMs by long("list_timeline_refresh") { 1500 }
            private val userTimelineMs by long("user_timeline_refresh") { 1500 }
            private val mentionTimelineMs by long("mention_timeline_refresh") { 30000 }
            private val homeTimelineMs by long("home_timeline_refresh") { 75000 }
            private val directMessageMs by long("direct_message_refresh") { 75000 }
            private val activityMs by long("activity_refresh") { 8000 }

            val listTimeline by lazy { listTimelineSec?.times(1000) ?: listTimelineMs }
            val userTimeline by lazy { userTimelineSec?.times(1000) ?: userTimelineMs }
            val mentionTimeline by lazy { mentionTimelineSec?.times(1000) ?: mentionTimelineMs }
            val homeTimeline by lazy { homeTimelineSec?.times(1000) ?: homeTimelineMs }
            val directMessage by lazy { directMessageSec?.times(1000) ?: directMessageMs }
            val activity by lazy { activitySec?.times(1000) ?: activityMs }
        }

        val twitter: PenicillinClient
            get() = PenicillinClient {
                account {
                    application(ck, cs)
                    token(at, ats)
                }
                skipEmulationChecking()
                httpClient(Apache)
            }

        val user by lazy {
            twitter.use {
                it.account.verifyCredentials().complete().result
            }
        }
        val friends by lazy {
            twitter.use {
                it.friend.listIds(count = 5000).untilLast().allIds
            }
        }
    }
}
