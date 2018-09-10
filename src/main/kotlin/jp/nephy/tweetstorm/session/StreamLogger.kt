package jp.nephy.tweetstorm.session

import jp.nephy.tweetstorm.TaskManager
import jp.nephy.tweetstorm.builder.newStatus
import jp.nephy.tweetstorm.logger
import org.slf4j.event.Level

class StreamLogger(private val manager: TaskManager, name: String) {
    private val logger = logger(name)
    
    suspend fun error(exception: Throwable? = null, stream: AuthenticatedStream? = null, text: () -> Any?) {
        if (exception != null) {
            logger.error(exception, text)
        } else {
            logger.error(text)
        }

        if (logger.isErrorEnabled) {
            emitStatus(stream, Level.ERROR, text)
        }
    }

    suspend fun warn(exception: Throwable? = null, stream: AuthenticatedStream? = null, text: () -> Any?) {
        if (exception != null) {
            logger.warn(exception, text)
        } else {
            logger.warn(text)
        }

        if (logger.isWarnEnabled) {
            emitStatus(stream, Level.WARN, text)
        }
    }
    
    suspend fun info(exception: Throwable? = null, stream: AuthenticatedStream? = null, text: () -> Any?) {
        if (exception != null) {
            logger.info(exception, text)
        } else {
            logger.info(text)
        }

        if (logger.isInfoEnabled) {
            emitStatus(stream, Level.INFO, text)
        }
    }

    suspend fun debug(exception: Throwable? = null, stream: AuthenticatedStream? = null, text: () -> Any?) {
        if (exception != null) {
            logger.debug(exception, text)
        } else {
            logger.debug(text)
        }

        if (logger.isDebugEnabled) {
            emitStatus(stream, Level.DEBUG, text)
        }
    }

    suspend fun trace(exception: Throwable? = null, stream: AuthenticatedStream? = null, text: () -> Any?) {
        if (exception != null) {
            logger.trace(exception, text)
        } else {
            logger.trace(text)
        }

        if (logger.isTraceEnabled) {
            emitStatus(stream, Level.TRACE, text)
        }
    }

    private suspend fun emitStatus(stream: AuthenticatedStream?, logLevel: Level, text: () -> Any?) {
        if (stream != null) {
            manager.emit(stream, newStatus {
                user {
                    name("[${logLevel.name}] Tweetstorm")
                }
                text(text)
            })
        } else {
            manager.emit(newStatus {
                user {
                    name("[${logLevel.name}] Tweetstorm")
                }
                text(text)
            })
        }
    }
}
