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
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

class Tweetstorm private constructor() {
    companion object {
        lateinit var config: Config
            private set

        val fetcher by lazy { Fetcher() }

        @JvmStatic
        fun main(args: Array<String>) {
            val cliArguments = parseCommandLine(args)
            config = Config.load(cliArguments.configPath)

            val environment = applicationEngineEnvironment {
                connector {
                    host = Tweetstorm.config.host
                    port = Tweetstorm.config.port
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
                        val logger = logger("Tweetstorm")
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

            embeddedServer(Netty, environment) {
                responseWriteTimeoutSeconds = 60
            }.start(wait = true)
        }
    }
}
