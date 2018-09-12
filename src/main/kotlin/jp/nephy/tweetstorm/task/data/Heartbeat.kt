package jp.nephy.tweetstorm.task.data

import jp.nephy.tweetstorm.session.StreamContent

class Heartbeat: ProduceData<Unit> {
    override val data: Unit
        get() = throw IllegalArgumentException()

    override suspend fun emit(handler: StreamContent.Handler): Boolean {
        return handler.heartbeat()
    }
}
