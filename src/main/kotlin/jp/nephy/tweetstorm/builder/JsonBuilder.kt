package jp.nephy.tweetstorm.builder

import jp.nephy.jsonkt.JsonModel

interface JsonBuilder<T: JsonModel> {
    fun build(): T
}
