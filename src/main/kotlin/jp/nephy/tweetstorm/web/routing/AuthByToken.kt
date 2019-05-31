package jp.nephy.tweetstorm.web.routing

import io.ktor.application.call
import io.ktor.html.respondHtmlTemplate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import jp.nephy.tweetstorm.session.PreAuthenticatedStream
import jp.nephy.tweetstorm.web.layout.NavLayout
import kotlinx.html.*

fun Route.authByToken() {
    route("/auth/token/{urlToken}") {
        get {
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

        post {
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