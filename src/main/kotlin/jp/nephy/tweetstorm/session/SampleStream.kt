package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest
import jp.nephy.jsonkt.JsonKt
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.builder.CustomStatusBuilder
import java.io.Writer
import java.util.*
import java.util.concurrent.TimeUnit

class SampleStream(writer: Writer, override val request: ApplicationRequest, override val account: Config.Account): StreamWriter(writer) {
    override fun handle() {
        while (isAlive) {
            val status = CustomStatusBuilder.new {
                text { UUID.randomUUID() }
            }

            send(JsonKt.toJsonString(status))
            TimeUnit.SECONDS.sleep(3)
        }
    }
}
