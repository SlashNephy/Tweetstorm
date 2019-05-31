package jp.nephy.tweetstorm

import io.ktor.application.Application
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import jp.nephy.tweetstorm.cli.parseCommandLine
import jp.nephy.tweetstorm.web.config
import jp.nephy.tweetstorm.web.module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlin.coroutines.CoroutineContext

object Tweetstorm: CoroutineScope {
    lateinit var config: Config
        private set

    private lateinit var dispatcher: CoroutineContext

    lateinit var httpClient: HttpClient
        private set

    override val coroutineContext: CoroutineContext
        get() = dispatcher

    @JvmStatic
    @ObsoleteCoroutinesApi
    fun main(args: Array<String>) {
        val cliArguments = parseCommandLine(args)

        config = Config.load(cliArguments.configPath)
        dispatcher = newFixedThreadPoolContext(config.app.parallelism, "Tweetstorm")
        httpClient = HttpClient(Apache)

        embeddedServer(Netty, host = config.wui.host, port = config.wui.port, configure = NettyApplicationEngine.Configuration::config, module = Application::module).start(wait = true)
    }
}
