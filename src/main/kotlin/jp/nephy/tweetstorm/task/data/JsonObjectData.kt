package jp.nephy.tweetstorm.task.data

import jp.nephy.jsonkt.ImmutableJsonObject
import jp.nephy.tweetstorm.session.StreamContent

class JsonObjectData(override val data: ImmutableJsonObject): ProduceData<ImmutableJsonObject> {
    override suspend fun emit(handler: StreamContent.Handler): Boolean {
        return handler.emit(data)
    }
}
