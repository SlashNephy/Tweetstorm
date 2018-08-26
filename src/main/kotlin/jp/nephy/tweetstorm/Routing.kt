package jp.nephy.tweetstorm

import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.response.respondWrite
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.toMap
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.session.PreAuthenticatedStream
import jp.nephy.tweetstorm.session.SampleStream
import kotlinx.html.*
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
            strict to true
        } else {
            call.request.headers.parseAuthorizationHeaderSimple() to false
        }

        try {
            call.respondWrite(ContentType.Application.Json, HttpStatusCode.OK) {
                if (account == null) {
                    logger.info { "Unknown client: ${call.request.origin.remoteHost} has connected." }
                    SampleStream(this, call.request).handle()
                    logger.info { "Unknown client: ${call.request.origin.remoteHost} has disconnected." }
                } else {
                    if (!strictAuth && !tweetstormConfig.skipAuth) {
                        logger.info { "Client: ${account.fullName} (${call.request.origin.remoteHost}) requested account-token authentication." }
                        val preStream = PreAuthenticatedStream(this, call.request, account)
                        preStream.handle()
                        if (!preStream.isSuccess) {
                            logger.warn { "Client: ${account.fullName} (${call.request.origin.remoteHost}) has failed account-token authentication. Close connection." }
                            return@respondWrite
                        }
                        logger.info { "Client: ${account.fullName} (${call.request.origin.remoteHost}) has passed account-token authentication. Keep connection." }
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
    route("/auth/token/{urlToken}") {
        get {
            call.respondHtml {
                body {
                    div {
                        p { +"Input your account token" }
                        form(method = FormMethod.post) {
                            input(type = InputType.password, name = "token")
                            input(type = InputType.submit) {
                                value = "Submit"
                            }
                        }
                    }
                }
            }
        }

        post {
            val urlToken = call.parameters["urlToken"] ?: return@post call.respond(HttpStatusCode.NotFound)
            val accountToken = call.receiveParameters()["token"]  ?: return@post call.respond(HttpStatusCode.Unauthorized)

            if (PreAuthenticatedStream.auth(urlToken, accountToken)) {
                call.respondText { "Your token is accepted. Streaming starts shortly. You may close this page for now." }
            } else {
                call.respondText(status = HttpStatusCode.Unauthorized) {
                    "Your token is invalid. Streaming cannot start for this account."
                }
            }
        }
    }
}
