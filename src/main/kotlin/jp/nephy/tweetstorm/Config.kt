package jp.nephy.tweetstorm

import ch.qos.logback.classic.Level
import jp.nephy.jsonkt.JsonObject
import jp.nephy.jsonkt.delegation.*
import jp.nephy.jsonkt.parse
import jp.nephy.jsonkt.stringOrNull
import jp.nephy.penicillin.PenicillinClient
import jp.nephy.penicillin.core.session.ApiClient
import jp.nephy.penicillin.core.session.config.*
import jp.nephy.penicillin.endpoints.account
import jp.nephy.penicillin.endpoints.account.verifyCredentials
import jp.nephy.penicillin.endpoints.common.TweetMode
import jp.nephy.penicillin.endpoints.friends
import jp.nephy.penicillin.endpoints.friends.listIds
import jp.nephy.penicillin.extensions.complete
import jp.nephy.penicillin.extensions.cursor.allIds
import jp.nephy.penicillin.extensions.cursor.untilLast
import kotlinx.io.core.use
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

    val wui by model<WebUI>()
    data class WebUI(override val json: JsonObject): JsonModel {
        val host by string { "127.0.0.1" }
        val port by int { 8080 }
        val maxConnections by nullableInt("max_connections")
    }

    val app by model<App>()
    data class App(override val json: JsonObject): JsonModel {
        val skipAuth by boolean("skip_auth") { false }
        val apiTimeout by long("api_timeout") { 3000 }
        val parallelism by int("parallelism") { maxOf(1, Runtime.getRuntime().availableProcessors() / 2) }
    }

    val logLevel by lambda("log_level") { Level.toLevel(it.stringOrNull, Level.INFO)!! }

    val accounts by modelList<Account>()
    data class Account(override val json: JsonObject): JsonModel {
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

        val filterStream by model<FilterStream>()
        data class FilterStream(override val json: JsonObject): JsonModel {
            val tracks by stringList("filter_stream_tracks")
            val follows by longList("filter_stream_follows")
        }

        val syncList by model<SyncList>()
        data class SyncList(override val json: JsonObject): JsonModel {
            val enabled by boolean("sync_list_following") { false }
            val includeSelf by boolean("sync_list_include_self") { true }
        }

        val t4i by model<T4iCredentials>()
        data class T4iCredentials(override val json: JsonObject): JsonModel {
            val at by nullableString("t4i_at")
            val ats by nullableString("t4i_ats")
        }

        val refresh by model<RefreshTime>()
        data class RefreshTime(override val json: JsonObject): JsonModel {
            val listTimeline by long("list_timeline") { 1500 }
            val userTimeline by long("user_timeline") { 1500 }
            val mentionTimeline by long("mention_timeline") { 30000 }
            val homeTimeline by long("home_timeline") { 75000 }
            val directMessage by long("direct_message") { 75000 }
            val activity by long("activity") { 8000 }
        }

        val twitter: ApiClient
            get() = PenicillinClient {
                account {
                    application(ck, cs)
                    token(at, ats)
                }
                httpClient(Tweetstorm.httpClient)
                api {
                    skipEmulationChecking()
                    defaultTweetMode = TweetMode.Extended
                }
            }

        val user by lazy {
            twitter.use {
                it.account.verifyCredentials().complete().result
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
