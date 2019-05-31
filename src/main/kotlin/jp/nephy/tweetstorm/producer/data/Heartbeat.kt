package jp.nephy.tweetstorm.producer.data

import jp.nephy.tweetstorm.session.StreamWriter

object Heartbeat: StreamData<Unit> {
    override val data: Unit
        get() = throw UnsupportedOperationException()

    override suspend fun emit(handler: StreamWriter): Boolean {
        return handler.heartbeat()
    }
}
