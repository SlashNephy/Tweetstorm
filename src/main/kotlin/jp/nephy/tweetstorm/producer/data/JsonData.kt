package jp.nephy.tweetstorm.producer.data

import jp.nephy.tweetstorm.session.StreamWriter

class JsonData(override vararg val data: Pair<String, Any?>): StreamData<Array<out Pair<String, Any?>>> {
    override suspend fun emit(handler: StreamWriter): Boolean {
        return handler.emit(*data)
    }
}
