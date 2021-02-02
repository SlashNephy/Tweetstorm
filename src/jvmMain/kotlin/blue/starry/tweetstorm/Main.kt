package blue.starry.tweetstorm

import io.ktor.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext

lateinit var config: Config
    private set
lateinit var dispatcher: ExecutorCoroutineDispatcher
    private set

@ObsoleteCoroutinesApi
fun main(args: Array<String>) {
    val cliArguments = parseCommandLine(args)
    config = Config.load(cliArguments.configPath)

    dispatcher = newFixedThreadPoolContext(config.app.parallelism, "Tweetstorm")

    embeddedServer(Netty, host = config.wui.host, port = config.wui.port, configure = NettyApplicationEngine.Configuration::config, module = Application::module).start(wait = true)
}
