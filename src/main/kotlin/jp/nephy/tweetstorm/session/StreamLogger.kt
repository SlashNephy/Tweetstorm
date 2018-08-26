package jp.nephy.tweetstorm.session

import jp.nephy.tweetstorm.TaskManager
import jp.nephy.tweetstorm.logger

class StreamLogger(private val manager: TaskManager, name: String) {
    private val logger = logger(name)
    
    fun error(exception: Throwable? = null, stream: AuthenticatedStream? = null, text: () -> Any?) {
        if (exception != null) {
            logger.error(exception, text)
        } else {
            logger.error(text)
        }

        if (stream != null) {
            manager.emit(stream) {
                user {
                    name("[Error] Tweetstorm")
                }
                text(text)
            }
        } else {
            manager.emit {
                user {
                    name("[Error] Tweetstorm")
                }
                text(text)
            }
        }
    }

    fun warn(exception: Throwable? = null, stream: AuthenticatedStream? = null, text: () -> Any?) {
        if (exception != null) {
            logger.warn(exception, text)
        } else {
            logger.warn(text)
        }

        if (stream != null) {
            manager.emit(stream) {
                user {
                    name("[Warn] Tweetstorm")
                }
                text(text)
            }
        } else {
            manager.emit {
                user {
                    name("[Warn] Tweetstorm")
                }
                text(text)
            }
        }
    }
    
    fun info(exception: Throwable? = null, stream: AuthenticatedStream? = null, text: () -> Any?) {
        if (exception != null) {
            logger.info(exception, text)
        } else {
            logger.info(text)
        }

        if (stream != null) {
            manager.emit(stream) {
                user {
                    name("[Info] Tweetstorm")
                }
                text(text)
            }
        } else {
            manager.emit {
                user {
                    name("[Info] Tweetstorm")
                }
                text(text)
            }
        }
    }

    fun debug(exception: Throwable? = null, stream: AuthenticatedStream? = null, text: () -> Any?) {
        if (exception != null) {
            logger.debug(exception, text)
        } else {
            logger.debug(text)
        }

        if (stream != null) {
            manager.emit(stream) {
                user {
                    name("[Debug] Tweetstorm")
                }
                text(text)
            }
        } else {
            manager.emit {
                user {
                    name("[Debug] Tweetstorm")
                }
                text(text)
            }
        }
    }

    fun trace(exception: Throwable? = null, stream: AuthenticatedStream? = null, text: () -> Any?) {
        if (exception != null) {
            logger.trace(exception, text)
        } else {
            logger.trace(text)
        }

        if (stream != null) {
            manager.emit(stream) {
                user {
                    name("[Trace] Tweetstorm")
                }
                text(text)
            }
        } else {
            manager.emit {
                user {
                    name("[Trace] Tweetstorm")
                }
                text(text)
            }
        }
    }
}
