package jp.nephy.tweetstorm

import mu.KLogger
import mu.KotlinLogging

fun logger(name: String): KLogger {
    return KotlinLogging.logger(name).also {
        (it.underlyingLogger as ch.qos.logback.classic.Logger).level = tweetstormConfig.logLevel
    }
}
