package blue.starry.tweetstorm.task.data

import blue.starry.tweetstorm.session.StreamContent

class Heartbeat: ProduceData<Unit> {
    override val data: Unit
        get() = throw IllegalArgumentException()

    override suspend fun emit(handler: StreamContent.Handler): Boolean {
        return handler.heartbeat()
    }
}
