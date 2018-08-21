package jp.nephy.tweetstorm

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.XForwardedHeadersSupport
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.response.respondWrite
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.toMap
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.session.SampleStream
import mu.KotlinLogging
import java.util.logging.LogManager

val logger = KotlinLogging.logger("Tweetstorm")
val config = Config.load()
val fetcher = Fetcher()

fun Application.module() {
    install(XForwardedHeadersSupport)

    install(Routing) {
        get("/") {
            call.respondText { "Tweetstorm is working fine. Have fun!" }
        }

        get("/1.1/user.json") {
            val account = call.request.headers.parseAuthorizationHeader(call.request.local.method, "https://userstream.twitter.com/1.1/user.json", call.request.queryParameters)
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

            call.respondWrite(ContentType.Application.Json, HttpStatusCode.OK) {
                if (account.debug) {
                    logger.warn { "Unknown user connected from ${call.request.origin.remoteHost} with parameter ${call.request.queryParameters.toMap()}." }
                    SampleStream(this, call.request.queryParameters)
                } else {
                    logger.info { "Account: @${account.sn} (ID: ${account.id}) connected from ${call.request.origin.remoteHost} with parameter ${call.request.queryParameters.toMap()}." }
                    AuthenticatedStream(this, call.request.queryParameters, account)
                }.handle()
            }
        }
    }
}

fun main(args: Array<String>) {
    LogManager.getLogManager().reset()
    val config = Config.load()

    embeddedServer(Netty, host = config.host, port = config.port, configure = {
        responseWriteTimeoutSeconds = 60
    }, module = Application::module).start(wait = true)
}
