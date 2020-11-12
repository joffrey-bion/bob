package org.hildan.github.secrets.wizard.http

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.util.*

fun ktorClient(
    logging: Boolean = false,
    configure: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {},
) = HttpClient(Apache) {
    followRedirects = true
    install(JsonFeature) {
        serializer = KotlinxSerializer(Json { ignoreUnknownKeys = true })
    }
    install(HttpCookies) {
        storage = UnencodedCookieStorage(AcceptAllCookiesStorage())
        useDefaultTransformers = false
    }
    if (logging) {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
    }
    configure()
}

// Prevents incorrect URL-encoding of cookies
// https://youtrack.jetbrains.com/issue/KTOR-917
private class UnencodedCookieStorage(private val cookiesStorage: CookiesStorage) : CookiesStorage by cookiesStorage {
    override suspend fun get(requestUrl: Url): List<Cookie> =
        cookiesStorage.get(requestUrl).map { it.copy(encoding = CookieEncoding.DQUOTES) }
}

fun HttpRequestBuilder.basicAuthHeader(login: String, password: String) {
    val basicAuth = "$login:$password".toBase64()
    header("Authorization", "Basic $basicAuth")
}

fun HttpRequestBuilder.oAuthHeader(token: String) {
    header("Authorization", "token $token")
}

fun httpFormUrlEncoded(vararg params: Pair<String, String>) =
    params.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, Charsets.UTF_8)}" }

fun String.toBase64(): String = Base64.getEncoder().encodeToString(toByteArray())