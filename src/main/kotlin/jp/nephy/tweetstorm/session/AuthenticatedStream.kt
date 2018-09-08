package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.Tweetstorm
import java.io.Writer

class AuthenticatedStream(writer: Writer, override val request: ApplicationRequest, val account: Config.Account): StreamWriter(writer) {
    override fun handle() {
        Tweetstorm.fetcher.start(this)
    }
}
