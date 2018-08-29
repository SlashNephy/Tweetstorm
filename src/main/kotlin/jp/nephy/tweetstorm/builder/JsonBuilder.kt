package jp.nephy.tweetstorm.builder

import com.google.gson.JsonObject
import jp.nephy.jsonkt.JsonModel

interface JsonBuilder<T: JsonModel> {
    val json: JsonObject
    fun build(): T
}
