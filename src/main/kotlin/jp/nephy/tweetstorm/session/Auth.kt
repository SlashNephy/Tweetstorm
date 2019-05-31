package jp.nephy.tweetstorm.session

import io.ktor.http.*
import jp.nephy.tweetstorm.Config
import jp.nephy.tweetstorm.Tweetstorm
import java.net.URLDecoder
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun Headers.parseAuthorizationHeaderStrict(method: HttpMethod, url: String, query: Parameters): Config.Account? {
    val authorization = get(HttpHeaders.Authorization) ?: return null
    if (!authorization.startsWith("OAuth")) {
        return null
    }

    val authorizationData = authorization.removePrefix("OAuth").split(",").asSequence().map {
        it.trim().split("=", limit = 2)
    }.map {
        it.first() to URLDecoder.decode(it.last(), Charsets.UTF_8.name()).removeSurrounding("\"")
    }.toMap()

    return Tweetstorm.config.accounts.asSequence().filter {
        it.ck == authorizationData["oauth_consumer_key"] && it.at == authorizationData["oauth_token"]
    }.find { account ->
        val signatureParam = authorizationData.toSortedMap()
        signatureParam.remove("oauth_signature")
        query.forEach { k, v ->
            signatureParam[k.encodeURLParameter()] = v.last().encodeURLParameter()
        }
        val signatureParamString = signatureParam.toList().joinToString("&") { "${it.first}=${it.second}" }.encodeOAuth()
        val signatureBaseString = "${method.value}&${url.encodeOAuth()}&$signatureParamString"

        val signingKey = SecretKeySpec("${account.cs.encodeOAuth()}&${account.ats.encodeOAuth()}".toByteArray(), "HmacSHA1")
        val signature = Mac.getInstance(signingKey.algorithm).apply {
            init(signingKey)
        }.doFinal(signatureBaseString.toByteArray()).let {
            Base64.getEncoder().encodeToString(it)
        }

        signature == authorizationData["oauth_signature"]
    }
}

fun Headers.parseAuthorizationHeaderSimple(): Config.Account? {
    val authorization = get(HttpHeaders.Authorization) ?: return null
    if (!authorization.startsWith("OAuth")) {
        return null
    }

    val authorizationData = authorization.removePrefix("OAuth").split(",").asSequence().map {
        it.trim().split("=", limit = 2)
    }.map {
        it.first() to URLDecoder.decode(it.last(), Charsets.UTF_8.name()).removeSurrounding("\"")
    }.toList().toMap()

    val id = authorizationData["oauth_token"]?.split("-")?.firstOrNull()?.toLongOrNull() ?: return null
    return Tweetstorm.config.accounts.find { it.user.id == id }
}
