package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.io.ByteWriteChannel
import java.io.Closeable

abstract class Stream(channel: ByteWriteChannel, val request: ApplicationRequest): Closeable {
    val job = Job()
    val writer = StreamWriter(channel, request)

    abstract suspend fun await(): Boolean

    override fun close() {
        job.cancel()
    }
}
