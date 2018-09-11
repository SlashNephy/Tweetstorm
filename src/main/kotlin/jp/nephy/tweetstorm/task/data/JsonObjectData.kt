package jp.nephy.tweetstorm.task.data

import com.google.gson.JsonObject
import jp.nephy.tweetstorm.session.StreamContent

class JsonObjectData(override val data: JsonObject): ProduceData<JsonObject> {
    override suspend fun emit(handler: StreamContent.Handler) {
        handler.emit(data)
    }
}
