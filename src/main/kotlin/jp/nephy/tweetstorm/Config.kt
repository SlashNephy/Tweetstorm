package jp.nephy.tweetstorm

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

    val host by json.byString { "localhost" }
    val port by json.byInt { 8080 }
    val accounts by json.byModelList<Account>()

    class Account(override val json: JsonObject): JsonModel {
        val debug by json.byBool { false }
        val markVia by json.byBool("mark_via") { true }
        val id by json.byLong
        val sn by json.byString
        val ck by json.byString
        val cs by json.byString
        val at by json.byString
        val ats by json.byString
        val listId by json.byNullableLong("list_id")
    }
}
