package jp.nephy.tweetstorm.producer.data

import jp.nephy.tweetstorm.session.StreamWriter

interface StreamData<T: Any> {
    val data: T

    suspend fun emit(handler: StreamWriter): Boolean
}
