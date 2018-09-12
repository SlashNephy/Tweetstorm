package jp.nephy.tweetstorm

import ch.qos.logback.classic.Level
import com.google.gson.JsonObject
import jp.nephy.jsonkt.*
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.allIds
import java.nio.file.Path
import java.nio.file.Paths

data class Config(override val json: JsonObject): JsonModel {
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
    data class WebUI(override val json: JsonObject): JsonModel {
        val host by json.byString { "127.0.0.1" }
        val port by json.byInt { 8080 }
        val maxConnections by json.byNullableInt("max_connections")
    }

    val app by lazy { App(json) }
    data class App(override val json: JsonObject): JsonModel {
        val skipAuth by json.byBool("skip_auth") { false }
        val apiTimeout by json.byLong("api_timeout") { 3000 }
        val commonPoolParallelism by json.byNullableInt("common_pool_parallelism")
    }

    val logLevel by lazy { Level.toLevel(json.getOrNull("log_level")?.toStringOrNull(), Level.INFO)!! }

    val accounts by json.byModelList<Account>()
    data class Account(override val json: JsonObject): JsonModel {
        val twitter by lazy {
            PenicillinClient {
                account {
                    application(ck, cs)
                    token(at, ats)
                }
                skipEmulationChecking()
            }
        }
        val user by lazy {
            twitter.account.verifyCredentials().complete().result
        }
        val friends by lazy {
            twitter.friend.listIds(count = 5000).complete().untilLast().allIds
        }

        val ck by json.byString
        val cs by json.byString
        val at by json.byString
        val ats by json.byString
        val listId by json.byNullableLong("list_id")
        val token by json.byNullableString

        val enableDirectMessage by json.byBool("enable_direct_message") { true }
        val enableActivity by json.byBool("enable_activity") { false }
        val enableFriends by json.byBool("enable_friends") { true }
        val enableSampleStream by json.byBool("enable_sample_stream") { false }

        val filterStream by lazy { FilterStream(json) }
        data class FilterStream(override val json: JsonObject): JsonModel {
            val tracks by json.byStringList("filter_stream_tracks")
            val follows by json.byLongList("filter_stream_follows")
        }

        val syncList by lazy { SyncList(json) }
        data class SyncList(override val json: JsonObject): JsonModel {
            val enabled by json.byBool("sync_list_following") { false }
            val includeSelf by json.byBool("sync_list_include_self") { true }
        }

        val t4i by lazy { T4iCredentials(json) }
        data class T4iCredentials(override val json: JsonObject): JsonModel {
            val at by json.byNullableString("t4i_at")
            val ats by json.byNullableString("t4i_ats")
        }

        val refresh by lazy { RefreshTime(json) }
        data class RefreshTime(override val json: JsonObject): JsonModel {
            val listTimeline by json.byLong("list_timeline_refresh") { 1500 }
            val userTimeline by json.byLong("user_timeline_refresh") { 1500 }
            val mentionTimeline by json.byLong("mention_timeline_refresh") { 30000 }
            val homeTimeline by json.byLong("home_timeline_refresh") { 75000 }
            val directMessage by json.byLong("direct_message_refresh") { 75000 }
            val activity by json.byLong("activity_refresh") { 8000 }
        }
    }
}
