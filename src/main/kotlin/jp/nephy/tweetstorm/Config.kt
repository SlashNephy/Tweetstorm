package jp.nephy.tweetstorm

import ch.qos.logback.classic.Level
import com.google.gson.JsonObject
import jp.nephy.jsonkt.*
import jp.nephy.penicillin.PenicillinClient
import java.nio.file.Paths

class Config(override val json: JsonObject): JsonModel {
    companion object {
        private val configPath = Paths.get("config.json")

        fun load(): Config {
            return try {
                configPath.parse()
            } catch (e: Exception) {
                throw IllegalStateException("config.json is invalid.")
            }
        }
    }

    val host by json.byString { "127.0.0.1" }
    val port by json.byInt { 8080 }

    val skipAuth by json.byBool("skip_auth") { false }

    val threadsPerAccount by json.byInt("threads_per_account") { -1 }

    private val logLevelString by json.byString("log_level") { "info" }
    val logLevel by lazy { Level.toLevel(logLevelString, Level.INFO)!! }

    val accounts by json.byModelList<Account>()

    class Account(override val json: JsonObject): JsonModel {
        val user by lazy {
            PenicillinClient {
                account {
                    application(ck, cs)
                    token(at, ats)
                }
            }.use {
                it.account.verifyCredentials().complete().result
            }
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

        val markVia by json.byBool("mark_via") { false }
        val markVote by json.byBool("mark_vote") { false }

        val listInterval by json.byInt("list_timeline_refresh_sec") { 3 }
        val homeInterval by json.byInt("home_timeline_refresh_sec") { 90 }
        val userInterval by json.byInt("user_timeline_refresh_sec") { 3 }
        val mentionInterval by json.byInt("mention_timeline_refresh_sec") { 45 }
        val messageInterval by json.byInt("direct_message_refresh_sec") { 90 }
        val activityInterval by json.byInt("activity_refresh_sec") { 10 }

        val enableActivity by json.byBool("enable_activity") { false }
        val twitterForiPhoneAccessToken by json.byNullableString("t4i_at")
        val twitterForiPhoneAccessTokenSecret by json.byNullableString("t4i_ats")
    }
}
