package jp.nephy.tweetstorm

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
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
import io.ktor.util.cio.ChannelWriteException
import io.ktor.util.toMap
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.session.SampleStream
import mu.KotlinLogging
import org.slf4j.event.Level
import java.io.IOException
import java.util.logging.LogManager

val config = Config.load()
val fetcher = Fetcher()

fun Application.module() {
    val logger = KotlinLogging.logger("Tweetstorm")
    install(CallLogging) {
        level = Level.INFO
    }

    install(XForwardedHeadersSupport)

    install(Routing) {
        get("/") {
            call.respondText { "Tweetstorm is working fine. Have fun!" }
        }

        get("/1.1/user.json") {
            val account = if (!config.skipAuth) {
                call.request.headers.parseAuthorizationHeader(call.request.local.method, "https://userstream.twitter.com/1.1/user.json", call.request.queryParameters)
            } else {
                call.request.headers.parseAuthorizationHeaderSimple()
            } ?: return@get call.respond(HttpStatusCode.Unauthorized)

            try {
                call.respondWrite(ContentType.Application.Json, HttpStatusCode.OK) {
                    logger.info { "Client: ${account.fullName} connected from ${call.request.origin.remoteHost} with parameter ${call.request.queryParameters.toMap()}." }
                    logger.debug { "Client: ${account.fullName} configuration = ${account.json}" }

                    if (account.debug) {
                        logger.info { "Client: ${account.fullName} connected to debug stream." }
                        SampleStream(this, call.request.queryParameters)
                    } else {
                        AuthenticatedStream(this, call.request.queryParameters, account)
                    }.start()

                    logger.info { "Client: ${account.fullName} has disconnected." }
                }
            } catch (e: IOException) {}
        }
    }
}

fun main(args: Array<String>) {
    LogManager.getLogManager().reset()

    embeddedServer(Netty, host = config.host, port = config.port, configure = {
        responseWriteTimeoutSeconds = 60
    }, module = Application::module).start(wait = true)
}
