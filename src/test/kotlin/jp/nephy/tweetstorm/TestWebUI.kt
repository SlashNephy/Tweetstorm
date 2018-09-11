package jp.nephy.tweetstorm

import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

private const val host = "localhost"
private const val port = 8080

class TestWebUI {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            embeddedServer(Netty, host = host, port = port) {
                install(CallLogging)
                install(Routing) {
                    getTop()
                }
            }.start(wait = true)
        }
    }
}
