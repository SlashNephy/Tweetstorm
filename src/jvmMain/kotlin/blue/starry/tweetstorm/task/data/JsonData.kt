package blue.starry.tweetstorm.task.data

import blue.starry.tweetstorm.session.StreamContent

class JsonData(override vararg val data: Pair<String, Any?>): ProduceData<Array<out Pair<String, Any?>>> {
    override suspend fun emit(handler: StreamContent.Handler): Boolean {
        return handler.emit(*data)
    }
}
