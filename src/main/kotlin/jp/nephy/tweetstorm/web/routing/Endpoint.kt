package jp.nephy.tweetstorm.web.routing

import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.routing.Route
import io.ktor.routing.get
import jp.nephy.tweetstorm.Tweetstorm
import jp.nephy.tweetstorm.session.parseAuthorizationHeaderSimple
import jp.nephy.tweetstorm.session.parseAuthorizationHeaderStrict
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.session.DemoStream
import jp.nephy.tweetstorm.session.PreAuthenticatedStream

fun Route.getUser() {
    get("/1.1/user.json") {
        val strict = call.request.headers.parseAuthorizationHeaderStrict(call.request.local.method, "https://userstream.twitter.com/1.1/user.json", call.request.queryParameters)
        val simple = call.request.headers.parseAuthorizationHeaderSimple()
        val account = strict ?: simple
        var authOK = strict != null || (Tweetstorm.config.app.skipAuth && account != null)

        call.respondStream { writer ->
            if (account != null && !Tweetstorm.config.app.skipAuth && !authOK) {
                PreAuthenticatedStream(writer, call.request, account).use { stream ->
                    if (stream.await()) {
                        authOK = true
                        logger.info { "Client: @${account.user.screenName} (${call.request.origin.remoteHost}) has passed account-token authentication." }
                    } else {
                        logger.warn { "Client: @${account.user.screenName} (${call.request.origin.remoteHost}) has failed account-token authentication." }
                    }
                }
            }

            if (account != null && authOK) {
                AuthenticatedStream(writer, call.request, account)
            } else {
                DemoStream(writer, call.request)
            }.use { stream ->
                stream.await()
            }
        }
    }
}