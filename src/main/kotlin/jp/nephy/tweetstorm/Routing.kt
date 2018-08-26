package jp.nephy.tweetstorm

import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.response.respondWrite
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.util.toMap
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.session.PreAuthenticatedStream
import jp.nephy.tweetstorm.session.SampleStream
import java.io.IOException

private val logger by lazy { logger("Tweetstorm.Routing") }

fun Route.getTop() {
    get("/") {
        call.respondText { "Tweetstorm is working fine. Have fun!" }
    }
}

fun Route.getUser() {
    get("/1.1/user.json") {
        val strict = call.request.headers.parseAuthorizationHeader(call.request.local.method, "https://userstream.twitter.com/1.1/user.json", call.request.queryParameters)
        val (account, strictAuth) = if (strict != null) {
            strict to false
        } else {
            call.request.headers.parseAuthorizationHeaderSimple() to false
        }

        try {
            call.respondWrite(ContentType.Application.Json, HttpStatusCode.OK) {
                if (account == null) {
                    SampleStream(this, call.request).handle()
                } else {
                    if (!strictAuth && !tweetstormConfig.skipAuth) {
                        val preStream = PreAuthenticatedStream(this, call.request, account)
                        preStream.handle()
                        if (!preStream.isSuccess) {
                            return@respondWrite
                        }
                    }

                    logger.info { "Client: ${account.fullName} (${call.request.origin.remoteHost}) connected with parameter ${call.request.queryParameters.toMap()}." }

                    AuthenticatedStream(this, call.request, account).handle()

                    logger.info { "Client: ${account.fullName} (${call.request.origin.remoteHost}) has disconnected." }
                }
            }
        } catch (e: IOException) {}
    }
}

fun Route.authByToken() {
    get("/auth/{token}") {
        val token = call.parameters["token"] ?: return@get call.respond(HttpStatusCode.NotFound)
        if (PreAuthenticatedStream.check(token)) {
            call.respondText { "Your token is accepted. Streaming starts shortly." }
        } else {
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}
