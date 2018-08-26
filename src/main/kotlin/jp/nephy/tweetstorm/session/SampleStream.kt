package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest
import jp.nephy.jsonkt.JsonKt
import jp.nephy.tweetstorm.builder.CustomStatusBuilder
import java.io.Writer
import java.util.concurrent.TimeUnit

class SampleStream(writer: Writer, override val request: ApplicationRequest): StreamWriter(writer) {
    override fun handle() {
        val status = CustomStatusBuilder.new {
            text { "This is sample stream. Since Tweetstorm could not authenticate you, sample stream has started. Please check your config.json." }
        }

        send(JsonKt.toJsonString(status))

        while (isAlive) {
            heartbeat()
            TimeUnit.SECONDS.sleep(10)
        }
    }
}
