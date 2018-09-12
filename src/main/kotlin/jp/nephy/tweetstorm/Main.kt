package jp.nephy.tweetstorm

import io.ktor.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine

lateinit var config: Config
    private set

fun main(args: Array<String>) {
    val cliArguments = parseCommandLine(args)
    config = Config.load(cliArguments.configPath)

    embeddedServer(Netty, host = config.wui.host, port = config.wui.port, configure = NettyApplicationEngine.Configuration::config, module = Application::module).start(wait = true)
}
