package jp.nephy.tweetstorm

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.response.respondWrite
import io.ktor.routing.Route
import io.ktor.routing.get
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.session.SampleStream
import java.io.IOException

fun Route.getTop() {
    get("/") {
        call.respondText { "Tweetstorm is working fine. Have fun!" }
    }
}

fun Route.getUser() {
    get("/1.1/user.json") {
        val account = if (!tweetstormConfig.skipAuth) {
            call.request.headers.parseAuthorizationHeader(call.request.local.method, "https://userstream.twitter.com/1.1/user.json", call.request.queryParameters)
        } else {
            call.request.headers.parseAuthorizationHeaderSimple()
        } ?: return@get call.respond(HttpStatusCode.Unauthorized)

        try {
            call.respondWrite(ContentType.Application.Json, HttpStatusCode.OK) {
                if (account.debug) {
                    SampleStream(this, call.request, account)
                } else {
                    AuthenticatedStream(this, call.request, account)
                }.handle()
            }
        } catch (e: IOException) {}
    }
}
