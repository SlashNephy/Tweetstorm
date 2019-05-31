package jp.nephy.tweetstorm.web.layout

import io.ktor.html.Placeholder
import io.ktor.html.Template
import io.ktor.html.insert
import kotlinx.html.*

class MainLayout: Template<HTML> {
    val content = Placeholder<HtmlBlockTag>()
    override fun HTML.apply() {
        head {
            meta(charset = "utf-8")
            meta("viewport", "width=device-width,initial-scale=1.0,minimum-scale=1.0,maximum-scale=1.0,user-scalable=no")
            title { +"Tweetstorm" }
            styleLink("https://cdnjs.cloudflare.com/ajax/libs/bootswatch/4.1.3/cosmo/bootstrap.min.css")
            styleLink("https://use.fontawesome.com/releases/v5.2.0/css/all.css")
        }
        body {
            insert(content)
        }
    }
}
