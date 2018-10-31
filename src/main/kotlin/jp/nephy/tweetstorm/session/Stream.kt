package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.io.ByteWriteChannel
import java.io.Closeable

abstract class Stream<T>(channel: ByteWriteChannel, val request: ApplicationRequest): Closeable {
    val job = Job()
    val handler = StreamContent.Handler(channel, request)

    abstract suspend fun await(): T

    override fun close() {
        runBlocking(Dispatchers.Default) {
            job.cancelChildren()
            job.cancelAndJoin()
        }
    }
}
