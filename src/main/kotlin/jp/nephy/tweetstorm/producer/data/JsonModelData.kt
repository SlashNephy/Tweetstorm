package jp.nephy.tweetstorm.producer.data

import jp.nephy.jsonkt.delegation.JsonModel
import jp.nephy.tweetstorm.session.StreamWriter

class JsonModelData(override val data: JsonModel): StreamData<JsonModel> {
    override suspend fun emit(handler: StreamWriter): Boolean {
        return handler.emit(data)
    }
}
