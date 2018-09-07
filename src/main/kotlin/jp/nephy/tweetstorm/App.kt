package jp.nephy.tweetstorm

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.netty.NettyApplicationEngine

val tweetstormConfig = Config.load()
val fetcher = Fetcher()
private val logger = logger("Tweetstorm")

fun main(args: Array<String>) {
    val environment = applicationEngineEnvironment {
        connector {
            host = tweetstormConfig.host
            port = tweetstormConfig.port
        }

        module {
            install(RequestLogging)
            install(XForwardedHeaderSupport)
            install(DefaultHeaders) {
                header(HttpHeaders.Server, "Tweetstorm")
            }
            install(Routing) {
                getTop()
                getUser()
                authByToken()
            }
            install(StatusPages) {
                exception<Exception> { e ->
                    logger.error(e) { "Internal server error occurred." }
                    call.respond(HttpStatusCode.InternalServerError)
                }

                status(HttpStatusCode.NotFound) {
                    notFound()
                }
            }
        }
    }

    NettyApplicationEngine(environment) {
        responseWriteTimeoutSeconds = 60
    }.start(wait = true)
}
