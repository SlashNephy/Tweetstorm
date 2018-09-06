package jp.nephy.tweetstorm

import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.html.respondHtmlTemplate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondWrite
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.toMap
import jp.nephy.tweetstorm.session.AuthenticatedStream
import jp.nephy.tweetstorm.session.PreAuthenticatedStream
import jp.nephy.tweetstorm.session.SampleStream
import kotlinx.html.*
import java.io.IOException

private val logger by lazy { logger("Tweetstorm.Routing") }

fun Route.getTop() {
    get("/") { _ ->
        call.respondHtmlTemplate(FooterLayout()) {
            footerContent {
                style {
                    unsafe {
                        +".github-corner:hover .octo-arm{animation:octocat-wave 560ms ease-in-out}@keyframes octocat-wave{0%,100%{transform:rotate(0)}20%,60%{transform:rotate(-25deg)}40%,80%{transform:rotate(10deg)}}@media (max-width:500px){.github-corner:hover .octo-arm{animation:none}.github-corner .octo-arm{animation:octocat-wave 560ms ease-in-out}}"
                    }
                }
                a("https://github.com/SlashNephy/Tweetstorm", "_blank", "github-corner") {
                    attributes["aria-label"] = "View source on Github"
                    unsafe { +"""<svg width="80" height="80" viewBox="0 0 250 250" style="fill:#151513; color:#fff; position: absolute; top: 0; border: 0; right: 0;" aria-hidden="true"><path d="M0,0 L115,115 L130,115 L142,142 L250,250 L250,0 Z"></path><path d="M128.3,109.0 C113.8,99.7 119.0,89.6 119.0,89.6 C122.0,82.7 120.5,78.6 120.5,78.6 C119.2,72.0 123.4,76.3 123.4,76.3 C127.3,80.9 125.5,87.3 125.5,87.3 C122.9,97.6 130.6,101.9 134.4,103.2" fill="currentColor" style="transform-origin: 130px 106px;" class="octo-arm"></path><path d="M115.0,115.0 C114.9,115.1 118.7,116.5 119.8,115.4 L133.7,101.6 C136.9,99.2 139.9,98.4 142.2,98.6 C133.8,88.0 127.5,74.4 143.8,58.0 C148.5,53.4 154.0,51.2 159.7,51.0 C160.3,49.4 163.2,43.6 171.4,40.1 C171.4,40.1 176.1,42.5 178.8,56.2 C183.1,58.6 187.2,61.8 190.9,65.4 C194.5,69.0 197.7,73.2 200.1,77.6 C213.8,80.2 216.3,84.9 216.3,84.9 C212.7,93.1 206.9,96.0 205.4,96.6 C205.1,102.4 203.0,107.8 198.3,112.5 C181.9,128.9 168.3,122.5 157.7,114.1 C157.9,116.9 156.7,120.9 152.7,124.9 L141.0,136.5 C139.8,137.7 141.6,141.9 141.8,141.8 Z" fill="currentColor" class="octo-body"></path></svg>""" }
                }
                div("jumbotron") {
                    h1 { +"Tweetstorm" }
                    hr("my-4")
                    p("lead") { +"A simple substitute implementation for the Twitter UserStream." }
                }
                div("container") {
                    p {
                        +"Tweetstorm is working fine "
                        span("far fa-smile-wink")
                        +" Have fun!"
                    }
                    div("list-group") {
                        a("https://github.com/SlashNephy/Tweetstorm", "_blank", "list-group-item list-group-item-action") {
                            span("fab fa-github")
                            +" GitHub"
                        }
                        a("https://github.com/SlashNephy/Tweetstorm/wiki/Setup-[ja]", "_blank", "list-group-item list-group-item-action") {
                            span("fas fa-book")
                            +" Setup [JA]"
                        }
                        a("https://twitter.com/SlashNephy", "_blank", "list-group-item list-group-item-action") {
                            span("fab fa-twitter")
                            +" Contact"
                        }
                    }
                }
            }
        }
    }
}

fun Route.getUser() {
    get("/1.1/user.json") {
        val strict = call.request.headers.parseAuthorizationHeaderStrict(call.request.local.method, "https://userstream.twitter.com/1.1/user.json", call.request.queryParameters)
        val simple = call.request.headers.parseAuthorizationHeaderSimple()
        val account = strict ?: simple
        var authOK = strict != null || (tweetstormConfig.skipAuth && account != null)

        try {
            call.respondWrite(ContentType.Application.Json, HttpStatusCode.OK) {
                if (account != null && !tweetstormConfig.skipAuth && !authOK) {
                    logger.info { "Client: @${account.user.screenName} (${call.request.origin.remoteHost}) requested account-token authentication." }

                    val preStream = PreAuthenticatedStream(this, call.request, account)
                    preStream.handle()

                    if (preStream.isSuccess) {
                        authOK = true
                        logger.info { "Client: @${account.user.screenName} (${call.request.origin.remoteHost}) has passed account-token authentication. Start streaming." }
                    } else {
                        logger.warn { "Client: @${account.user.screenName} (${call.request.origin.remoteHost}) has failed account-token authentication." }
                    }
                }

                if (account != null && authOK) {
                    logger.info { "Client: @${account.user.screenName} (${call.request.origin.remoteHost}) connected with parameter ${call.request.queryParameters.toMap()}." }
                    AuthenticatedStream(this, call.request, account).handle()
                    logger.info { "Client: @${account.user.screenName} (${call.request.origin.remoteHost}) has disconnected." }
                } else {
                    logger.info { "Unknown client: ${call.request.origin.remoteHost} has connected." }
                    SampleStream(this, call.request).handle()
                    logger.info { "Unknown client: ${call.request.origin.remoteHost} has disconnected." }
                }
            }
        } catch (e: IOException) {
        }
    }
}

fun Route.authByToken() {
    route("/auth/token/{urlToken}") {
        get { _ ->
            val urlToken = call.parameters["urlToken"]
            if (urlToken == null || !PreAuthenticatedStream.check(urlToken)) {
                call.respondHtmlTemplate(NavLayout()) {
                    navContent {
                        div("alert alert-dismissible alert-danger") {
                            h4 {
                                span("far fa-sad-tear")
                                +" Requested auth token is invalid."
                            }
                            p { +"Try connecting from client you want to use." }
                        }
                    }

                }
            } else {
                call.respondHtmlTemplate(NavLayout()) {
                    navContent {
                        div("alert alert-dismissible alert-warning") {
                            h4 {
                                span("far fa-surprise")
                                +" Authentication Request"
                            }
                            p {
                                +"Enter your account token which you defined in "
                                code { +"config.json" }
                                +"."
                            }
                        }

                        form(method = FormMethod.post) {
                            attributes["data-bitwarden-watching"] = "1"
                            div("form-group") {
                                label {
                                    attributes["for"] = "token"
                                    +"Token"
                                }
                                input(type = InputType.password, name = "token", classes = "form-control") {
                                    attributes["id"] = "token"
                                }
                            }
                            input(type = InputType.submit, classes = "btn btn-primary") {
                                value = "Submit"
                            }
                        }
                    }
                }
            }
        }

        post { _ ->
            val urlToken = call.parameters["urlToken"]
            if (urlToken == null || !PreAuthenticatedStream.check(urlToken)) {
                return@post call.respond(HttpStatusCode.NotFound)
            }
            val accountToken = call.receiveParameters()["token"]
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

            if (PreAuthenticatedStream.auth(urlToken, accountToken)) {
                call.respondHtmlTemplate(NavLayout()) {
                    navContent {
                        div("alert alert-dismissible alert-success") {
                            h4 {
                                span("far fa-grin-squint")
                                +" Your token is accepted!"
                            }
                            p { +"Streaming starts shortly. Enjoy!" }
                        }
                    }
                }
            } else {
                call.respondHtmlTemplate(NavLayout(), HttpStatusCode.Unauthorized) {
                    navContent {
                        div("alert alert-dismissible alert-danger") {
                            h4 {
                                span("far fa-sad-tear")
                                +" Your token is invalid!"
                            }
                            p { +"Streaming can't start for this session." }
                        }
                        form(method = FormMethod.post) {
                            attributes["data-bitwarden-watching"] = "1"
                            div("form-group") {
                                label {
                                    attributes["for"] = "token"
                                    +"Token"
                                }
                                input(type = InputType.password, name = "token", classes = "form-control") {
                                    attributes["id"] = "token"
                                }
                            }
                            input(type = InputType.submit, classes = "btn btn-primary") {
                                value = "Submit"
                            }

                        }
                    }
                }
            }
        }
    }
}
