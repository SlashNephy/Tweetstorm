package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest
import jp.nephy.jsonkt.toJsonString
import jp.nephy.tweetstorm.builder.newStatus
import java.io.Writer
import java.util.concurrent.TimeUnit

class DemoStream(writer: Writer, override val request: ApplicationRequest): StreamWriter(writer) {
    override fun handle() {
        val status = newStatus {
            text { "This is demo stream. Since Tweetstorm could not authenticate you, demo stream has started. Please check your config.json." }
        }

        send(status.toJsonString())

        while (isAlive) {
            heartbeat()
            TimeUnit.SECONDS.sleep(10)
        }
    }
}
