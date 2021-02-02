package blue.starry.tweetstorm

import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.routing.Routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer

private const val host = "localhost"
private const val port = 8080

class TestWebUI {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            embeddedServer(CIO, host = host, port = port) {
                install(CallLogging)
                install(Routing) {
                    getTop()
                }
            }.start(wait = true)
        }
    }
}
