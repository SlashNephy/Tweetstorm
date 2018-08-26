package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.fetcher
import java.io.Writer

class AuthenticatedStream(writer: Writer, override val request: ApplicationRequest, override val account: Config.Account): StreamWriter(writer) {
    override fun handle() {
        fetcher.start(this)
    }
}
