package jp.nephy.tweetstorm.session

import io.ktor.request.ApplicationRequest

interface Stream {
    val request: ApplicationRequest

    fun handle()
}
