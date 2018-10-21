package jp.nephy.tweetstorm.builder

import jp.nephy.jsonkt.MutableJsonObject
import jp.nephy.jsonkt.delegation.JsonModel

interface JsonBuilder<T: JsonModel> {
    val json: MutableJsonObject
    fun build(): T
}
