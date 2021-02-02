package blue.starry.tweetstorm.task.data

import blue.starry.jsonkt.delegation.JsonModel
import blue.starry.tweetstorm.session.StreamContent

class JsonModelData(override val data: JsonModel): ProduceData<JsonModel> {
    override suspend fun emit(handler: StreamContent.Handler): Boolean {
        return handler.emit(data)
    }
}
