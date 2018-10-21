package jp.nephy.tweetstorm.task.data

import jp.nephy.jsonkt.delegation.JsonModel
import jp.nephy.tweetstorm.session.StreamContent

class JsonModelData(override val data: JsonModel): ProduceData<JsonModel> {
    override suspend fun emit(handler: StreamContent.Handler): Boolean {
        return handler.emit(data)
    }
}
