package jp.nephy.tweetstorm.web.layout

import io.ktor.html.Placeholder
import io.ktor.html.Template
import io.ktor.html.insert
import kotlinx.html.*

class FooterLayout: Template<HTML> {
    val footerContent = Placeholder<HtmlBlockTag>()
    override fun HTML.apply() {
        insert(MainLayout()) {
            content {
                insert(footerContent)
                div("container") {
                    hr()
                    div("text-center") {
                        style {
                            unsafe {
                                +".opacity05 { opacity: 0.5; }"
                            }
                        }
                        p {
                            +"Tweetstorm brought to you by "
                            a("https://github.com/SlashNephy") {
                                +"@SlashNephy"
                            }
                            +", "
                            a("https://github.com/motitaiyaki") {
                                +"@motitaiyaki"
                            }
                            +" and "
                            a("https://github.com/suzutan") {
                                +"@suzutan"
                            }
                            +" with "
                            span("fas fa-heart")
                            br()
                            span("opacity05") {
                                +"...and "
                                span("fas fa-angry")
                                +" to "
                                span("fab fa-twitter")
                            }
                        }
                    }
                }
            }
        }
    }
}