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

    val host by json.byString { "127.0.0.1" }
    val port by json.byInt { 8080 }

    val skipAuth by json.byBool("skip_auth") { false }

    val maxConnections by json.byInt("max_connections") { 2 * accounts.size }

    val apiTimeoutSec by json.byLong("api_timeout_sec") { 3 }

    private val logLevelString by json.byString("log_level") { "info" }
    val logLevel by lazy { Level.toLevel(logLevelString, Level.INFO)!! }

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

        val syncListFollowing by json.byBool("sync_list_following") { false }
        val syncListIncludeSelf by json.byBool("sync_list_include_self") { true }

        val enableDirectMessage by json.byBool("enable_direct_message") { true }
        val enableFriends by json.byBool("enable_friends") { true }
        val enableSampleStream by json.byBool("enable_sample_stream") { false }

        val filterStreamTracks by json.byStringList("filter_stream_tracks")
        val filterStreamFollows by json.byLongList("filter_stream_follows")

        val listInterval by json.byLong("list_timeline_refresh_sec") { 3 }
        val homeInterval by json.byLong("home_timeline_refresh_sec") { 90 }
        val userInterval by json.byLong("user_timeline_refresh_sec") { 3 }
        val mentionInterval by json.byLong("mention_timeline_refresh_sec") { 45 }
        val messageInterval by json.byLong("direct_message_refresh_sec") { 90 }
        val activityInterval by json.byLong("activity_refresh_sec") { 10 }

        val enableActivity by json.byBool("enable_activity") { false }
        val twitterForiPhoneAccessToken by json.byNullableString("t4i_at")
        val twitterForiPhoneAccessTokenSecret by json.byNullableString("t4i_ats")
    }
}
