package blue.starry.tweetstorm.task.data

import blue.starry.tweetstorm.session.StreamContent

interface ProduceData<T> {
    val data: T
    suspend fun emit(handler: StreamContent.Handler): Boolean
}
