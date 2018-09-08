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
        (it.underlyingLogger as ch.qos.logback.classic.Logger).level = Tweetstorm.config.logLevel
    }
}

private val logger = logger("Tweetstorm.Routing")

internal class RequestLogging private constructor(private val monitor: ApplicationEvents) {
    private val shouldIgnore = { call: ApplicationCall ->
        call.request.httpMethod == HttpMethod.Get && call.request.path() == "/1.1/user.json"
    }

    private val onStart = { _: Application ->
        logger.info("Application is responding at http://${Tweetstorm.config.host}:${Tweetstorm.config.port}")
    }
    private var onStop = { _: Application ->
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
