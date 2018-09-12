package jp.nephy.tweetstorm.task.data

import jp.nephy.tweetstorm.session.StreamContent

class JsonData(override vararg val data: Pair<String, Any?>): ProduceData<Array<out Pair<String, Any?>>> {
    override suspend fun emit(handler: StreamContent.Handler): Boolean {
        return handler.emit(*data)
    }
}
