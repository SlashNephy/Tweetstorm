package jp.nephy.tweetstorm.task.data

import jp.nephy.jsonkt.JsonObject
import jp.nephy.tweetstorm.session.StreamContent

class JsonObjectData(override val data: JsonObject): ProduceData<JsonObject> {
    override suspend fun emit(handler: StreamContent.Handler): Boolean {
        return handler.emit(data)
    }
}
