package jp.nephy.tweetstorm.task.data

import jp.nephy.tweetstorm.session.StreamContent

interface ProduceData<T> {
    val data: T
    suspend fun emit(handler: StreamContent.Handler)
}
