package jp.nephy.tweetstorm

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun Headers.parseAuthorizationHeader(method: HttpMethod, url: String, query: Parameters): Config.Account? {
    val authorization = get(HttpHeaders.Authorization) ?: return null
    if (!authorization.startsWith("OAuth")) {
        return null
    }

    val authorizationData = authorization.removePrefix("OAuth").split(",").map {
        it.trim().split("=", limit = 2)
    }.map {
        it.first() to URLDecoder.decode(it.last(), Charsets.UTF_8.name()).removeSurrounding("\"")
    }.toMap()

    return config.accounts.filter {
        it.ck == authorizationData["oauth_consumer_key"] && it.at == authorizationData["oauth_token"]
    }.find {
        val signatureParam = authorizationData.toSortedMap()
        signatureParam.remove("oauth_signature")
        query.forEach { k, v ->
            signatureParam[k.toURLEncode()] = v.last().toURLEncode()
        }
        val signatureParamString = signatureParam.toList().joinToString("&") { "${it.first}=${it.second}" }.toURLEncode()
        val signatureBaseString = "${method.value}&${url.toURLEncode()}&$signatureParamString"

        val signingKey = SecretKeySpec("${it.cs.toURLEncode()}&${it.ats.toURLEncode()}".toByteArray(), "HmacSHA1")
        val signature = Mac.getInstance(signingKey.algorithm).apply {
            init(signingKey)
        }.doFinal(signatureBaseString.toByteArray()).let {
            Base64.getEncoder().encodeToString(it)
        }

        signature == authorizationData["oauth_signature"]
    }
}

private fun String.toURLEncode(): String {
    return URLEncoder.encode(this@toURLEncode, "UTF-8").replace("%7E", "~").map {
        when (it) {
            '+' -> "%20"
            '*' -> "%2A"
            else -> "$it"
        }
    }.joinToString("")
}
