package jp.nephy.tweetstorm.producer.data

import jp.nephy.jsonkt.JsonObject
import jp.nephy.tweetstorm.session.StreamWriter

class JsonObjectData(override val data: JsonObject): StreamData<JsonObject> {
    override suspend fun emit(handler: StreamWriter): Boolean {
        return handler.emit(data)
    }
}
