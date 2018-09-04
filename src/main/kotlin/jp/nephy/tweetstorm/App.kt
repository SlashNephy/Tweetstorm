package jp.nephy.tweetstorm

import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.routing.Routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.netty.NettyApplicationEngine
import mu.KotlinLogging
import org.slf4j.event.Level

private val logger = KotlinLogging.logger("Tweetstorm")
val tweetstormConfig = Config.load()
val fetcher = Fetcher()

fun main(args: Array<String>) {
    val environment = applicationEngineEnvironment {
        log = logger

        connector {
            host = tweetstormConfig.host
            port = tweetstormConfig.port
        }

        module {
            install(CallLogging) {
                level = Level.INFO
            }
            install(XForwardedHeaderSupport)

            install(Routing) {
                getTop()
                getUser()
                authByToken()
            }
        }
    }

    NettyApplicationEngine(environment) {
        responseWriteTimeoutSeconds = 60
    }.start(wait = true)
}
