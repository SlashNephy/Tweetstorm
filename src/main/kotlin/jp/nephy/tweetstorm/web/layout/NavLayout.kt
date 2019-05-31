package jp.nephy.tweetstorm.web.layout

import io.ktor.html.Placeholder
import io.ktor.html.Template
import io.ktor.html.insert
import kotlinx.html.*

class NavLayout: Template<HTML> {
    val navContent = Placeholder<HtmlBlockTag>()
    override fun HTML.apply() {
        insert(FooterLayout()) {
            footerContent {
                div("navbar navbar-expand-lg navbar-dark bg-primary") {
                    a("/", "", "navbar-brand") { +"Tweetstorm" }
                }
                style {
                    unsafe {
                        +".alert { padding-top: 16px; }"
                    }
                }
                div("container") {
                    insert(navContent)
                }
            }
        }
    }
}