package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import java.io.Closeable
import java.io.Writer

abstract class Stream<T>(channel: ByteWriteChannel, val request: ApplicationRequest): Closeable {
    val job = Job()
    val handler = StreamContent.Handler(channel, request)

    abstract suspend fun await(): T

    override fun close() {
        runBlocking(CommonPool) {
            job.cancelChildren()
            job.cancelAndJoin()
        }
    }
}
