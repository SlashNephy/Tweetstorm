package blue.starry.tweetstorm.task.data

import blue.starry.jsonkt.JsonObject
import blue.starry.tweetstorm.session.StreamContent

class JsonObjectData(override val data: JsonObject): ProduceData<JsonObject> {
    override suspend fun emit(handler: StreamContent.Handler): Boolean {
        return handler.emit(data)
    }
}
