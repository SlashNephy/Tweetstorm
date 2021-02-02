package blue.starry.tweetstorm

import io.ktor.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer

lateinit var config: Config
    private set

fun main(args: Array<String>) {
    val cliArguments = parseCommandLine(args)
    config = Config.load(cliArguments.configPath)

    embeddedServer(CIO, host = config.wui.host, port = config.wui.port, module = Application::module).start(wait = true)
}
