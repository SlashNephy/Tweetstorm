package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.cancelAndJoin
import kotlinx.coroutines.experimental.runBlocking
import java.io.Closeable
import java.io.Writer

abstract class Stream<T>(writer: Writer, val request: ApplicationRequest): Closeable {
    val job = Job()
    val handler = StreamContent.Handler(writer, request)

    abstract suspend fun await(): T

    override fun close() {
        runBlocking(CommonPool) {
            job.children.filter { it.isActive }.forEach {
                it.cancelAndJoin()
            }
            job.cancelAndJoin()
        }
    }
}
