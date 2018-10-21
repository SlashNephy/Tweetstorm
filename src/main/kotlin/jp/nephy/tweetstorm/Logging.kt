package jp.nephy.tweetstorm

import io.ktor.application.*
import io.ktor.features.origin
import io.ktor.http.HttpMethod
import io.ktor.pipeline.PipelinePhase
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.userAgent
import io.ktor.util.AttributeKey
import mu.KLogger
import mu.KotlinLogging

fun logger(name: String): KLogger {
    return KotlinLogging.logger(name).also {
        (it.underlyingLogger as ch.qos.logback.classic.Logger).level = config.logLevel
    }
}

private val logger = logger("Tweetstorm.Routing")

internal class RequestLogging private constructor(private val monitor: ApplicationEvents) {
    private val shouldIgnore = { call: ApplicationCall ->
        call.request.httpMethod == HttpMethod.Get && call.request.path() == "/1.1/user.json"
    }

    private val onStart: (Application) -> Unit = {
        logger.info("Application is responding at http://${config.wui.host}:${config.wui.port}")
    }
    private var onStop: (Application) -> Unit = {
        logger.info("Application stopped.")
    }

    init {
        onStop = {
            onStop(it)
            monitor.unsubscribe(ApplicationStarted, onStart)
            monitor.unsubscribe(ApplicationStopped, onStop)
        }

        monitor.subscribe(ApplicationStarted, onStart)
        monitor.subscribe(ApplicationStopped, onStop)
    }

    companion object Feature : ApplicationFeature<Application, Unit, RequestLogging> {
        override val key: AttributeKey<RequestLogging> = AttributeKey("RequestLogging")

        override fun install(pipeline: Application, configure: Unit.() -> Unit): RequestLogging {
            val phase = PipelinePhase("RequestLogging")
            val feature = RequestLogging(pipeline.environment.monitor)

            pipeline.insertPhaseBefore(ApplicationCallPipeline.Infrastructure, phase)
            pipeline.intercept(phase) {
                proceed()
                if (!feature.shouldIgnore(call)) {
                    logger.info { "[${call.response.status()?.value ?: " - "}] ${call.request.httpMethod.value.toUpperCase()} ${call.request.path()}\nfrom ${call.request.origin.remoteHost} with \"${call.request.userAgent().orEmpty()}\"" }
                }
            }
            return feature
        }
    }
}
