package jp.nephy.tweetstorm.session

import io.ktor.features.origin
import io.ktor.request.ApplicationRequest
import io.ktor.util.toMap
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.StreamManager
import jp.nephy.tweetstorm.logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.io.ByteWriteChannel
import kotlinx.coroutines.sync.withLock

private val logger = logger("Tweetstorm.AuthenticatedStream")

class AuthenticatedStream(channel: ByteWriteChannel,request: ApplicationRequest, val account: Config.Account): Stream(channel, request) {
    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    override suspend fun await(): Boolean {
        logger.info { "Client: @${account.user.screenName} (${request.origin.remoteHost}) connected to UserStream API with parameter ${request.queryParameters.toMap()}." }

        val manager = StreamManager.register(this)

        manager.wait(this)

        logger.info { "Client: @${account.user.screenName} (${request.origin.remoteHost}) has disconnected from UserStream API." }
        return false
    }
}
