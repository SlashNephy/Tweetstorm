package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest
import jp.nephy.tweetstorm.Config

interface Stream {
    val request: ApplicationRequest
    val account: Config.Account

    fun handle()
}
