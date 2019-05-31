package jp.nephy.tweetstorm.web.routing

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.html.respondHtmlTemplate
import io.ktor.http.HttpStatusCode
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.util.pipeline.PipelineContext
import jp.nephy.tweetstorm.web.layout.NavLayout
import kotlinx.html.div
import kotlinx.html.h4
import kotlinx.html.p
import kotlinx.html.span

suspend fun PipelineContext<*, ApplicationCall>.notFound() {
    call.respondHtmlTemplate(NavLayout(), HttpStatusCode.NotFound) {
        navContent {
            div("alert alert-dismissible alert-danger") {
                h4 {
                    span("far fa-sad-tear")
                    +" 404 Page Not Found"
                }
                p { +"${call.request.httpMethod.value.toUpperCase()} ${call.request.path()}" }
            }
        }
    }
}