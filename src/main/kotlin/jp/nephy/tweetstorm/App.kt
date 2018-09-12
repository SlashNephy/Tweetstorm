package jp.nephy.tweetstorm

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.server.netty.NettyApplicationEngine

private val logger = jp.nephy.tweetstorm.logger("Tweetstorm.App")

fun Application.module() {
    install(RequestLogging)
    install(XForwardedHeaderSupport)
    install(DefaultHeaders) {
        header(HttpHeaders.Server, "Tweetstorm")
    }
    // install(Compression)
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

fun NettyApplicationEngine.Configuration.config() {
    responseWriteTimeoutSeconds = 60

    val maxConnectionsOverride = maxOf(config.maxConnections, 2)
    connectionGroupSize = maxConnectionsOverride
    workerGroupSize = maxConnectionsOverride
    callGroupSize = 2 * (maxConnectionsOverride - 1)

    logger.debug { "parallelism = $parallelism, connectionGroupSize = workerGroupSize = $connectionGroupSize, callGroupSize = $callGroupSize" }
}
