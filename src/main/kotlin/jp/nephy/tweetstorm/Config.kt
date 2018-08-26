package jp.nephy.tweetstorm

import ch.qos.logback.classic.Level
import com.google.gson.JsonObject
import jp.nephy.jsonkt.*
import java.nio.file.Paths

class Config(override val json: JsonObject): JsonModel {
    companion object {
        private val configPath = Paths.get("config.json")

        fun load(): Config {
            return try {
                JsonKt.parse(configPath)
            } catch (e: Exception) {
                throw IllegalStateException("config.json is invalid.")
            }
        }
    }

    val host by json.byString { "127.0.0.1" }
    val port by json.byInt { 8080 }
    val skipAuth by json.byBool("skip_auth") { false }
    val logLevel by json.byLambda("log_level") { Level.toLevel(toStringOrDefault("info"), Level.INFO)!! }
    val accounts by json.byModelList<Account>()

    class Account(override val json: JsonObject): JsonModel {
        val id by json.byLong
        private val sn by json.byString
        val displayName by lazy { "@$sn" }
        val fullName by lazy { "$displayName (ID: $id)" }
        val ck by json.byString
        val cs by json.byString
        val at by json.byString
        val ats by json.byString
        val listId by json.byNullableLong("list_id")

        val debug by json.byBool { false }
        val syncListFollowing by json.byBool("sync_list_following") { false }
        val syncListIncludeSelf by json.byBool("sync_list_include_self") { true }
        val enableFriends by json.byBool("enable_friends") { true }
        val markVia by json.byBool("mark_via") { false }
        val markVote by json.byBool("mark_vote") { false }
        val listInterval by json.byInt("list_timeline_refresh_sec") { 3 }
        val homeInterval by json.byInt("home_timeline_refresh_sec") { 90 }
        val userInterval by json.byInt("user_timeline_refresh_sec") { 3 }
        val mentionInterval by json.byInt("mention_timeline_refresh_sec") { 45 }
    }
}
