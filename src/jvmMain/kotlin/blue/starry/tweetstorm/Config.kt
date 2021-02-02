package blue.starry.tweetstorm

import blue.starry.jsonkt.JsonObject
import blue.starry.jsonkt.delegation.*
import blue.starry.jsonkt.parseObject
import blue.starry.jsonkt.stringOrNull
import blue.starry.penicillin.PenicillinClient
import blue.starry.penicillin.core.session.ApiClient
import blue.starry.penicillin.core.session.config.account
import blue.starry.penicillin.core.session.config.application
import blue.starry.penicillin.core.session.config.token
import blue.starry.penicillin.endpoints.account
import blue.starry.penicillin.endpoints.account.verifyCredentials
import blue.starry.penicillin.endpoints.friends
import blue.starry.penicillin.endpoints.friends.listIds
import blue.starry.penicillin.extensions.complete
import blue.starry.penicillin.extensions.cursor.allIds
import blue.starry.penicillin.extensions.cursor.untilLast
import ch.qos.logback.classic.Level
import java.nio.file.Path
import java.nio.file.Paths

private val logger = blue.starry.tweetstorm.logger("Tweetstorm.Config")

data class Config(override val json: JsonObject): JsonModel {
    companion object {
        private val defaultConfigPath = Paths.get("config.json")

        fun load(configPath: Path?): Config {
            return try {
                (configPath ?: defaultConfigPath).toFile().readText().parseObject { Config(it) }
            } catch (e: Exception) {
                throw IllegalStateException("config.json is invalid.")
            }
        }
    }

    val wui by lazy { WebUI(json) }
    data class WebUI(override val json: JsonObject): JsonModel {
        val host by string { "127.0.0.1" }
        val port by int { 8080 }
        val maxConnections by nullableInt("max_connections")
    }

    val app by lazy { App(json) }
    data class App(override val json: JsonObject): JsonModel {
        val skipAuth by boolean("skip_auth") { false }
        val apiTimeout by long("api_timeout") { 3000 }
    }

    val logLevel by lambda("log_level", Level.INFO) { Level.toLevel(it.stringOrNull, Level.INFO) }

    val accounts by modelList { Account(it) }
    data class Account(override val json: JsonObject): JsonModel {
        val ck by string
        val cs by string
        val at by string
        val ats by string
        val listId by nullableLong("list_id")
        val token by nullableString

        val enableDirectMessage by boolean("enable_direct_message") { true }
        val enableFriends by boolean("enable_friends") { true }
        val enableSampleStream by boolean("enable_sample_stream") { false }

        val filterStream by lazy { FilterStream(json) }
        data class FilterStream(override val json: JsonObject): JsonModel {
            val tracks by stringList("filter_stream_tracks")
            val follows by longList("filter_stream_follows")
        }

        val syncList by lazy { SyncList(json) }
        data class SyncList(override val json: JsonObject): JsonModel {
            val enabled by boolean("sync_list_following") { false }
            val includeSelf by boolean("sync_list_include_self") { true }
        }

        val refresh by lazy { RefreshTime(json) }
        data class RefreshTime(override val json: JsonObject): JsonModel {
            val listTimeline by long("list_timeline_refresh") { 1500 }
            val userTimeline by long("user_timeline_refresh") { 1500 }
            val mentionTimeline by long("mention_timeline_refresh") { 30000 }
            val homeTimeline by long("home_timeline_refresh") { 75000 }
            val directMessage by long("direct_message_refresh") { 75000 }
        }

        val twitter: ApiClient
            get() = PenicillinClient {
                account {
                    application(ck, cs)
                    token(at, ats)
                }
            }

        val user by lazy {
            twitter.use {
                it.account.verifyCredentials.complete().result
            }
        }
        val friends by lazy {
            twitter.use {
                runCatching {
                    it.friends.listIds(count = 5000).untilLast().allIds
                }.getOrNull().orEmpty()
            }
        }
    }
}
