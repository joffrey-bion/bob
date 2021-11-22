package org.hildan.bob.http

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.util.*

val http = ktorClient()

fun ktorClient(
    logging: Boolean = false,
    configure: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {},
) = HttpClient(Apache) {
    followRedirects = true
    install(JsonFeature) {
        serializer = KotlinxSerializer(Json { ignoreUnknownKeys = true })
    }
    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
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

fun HttpRequestBuilder.basicAuthHeader(login: String, password: String) {
    val basicAuth = "$login:$password".toBase64()
    header("Authorization", "Basic $basicAuth")
}

fun HttpRequestBuilder.bearerAuthHeader(token: String) {
    header("Authorization", "Bearer $token")
}

fun HttpRequestBuilder.tokenAuthHeader(token: String) {
    header("Authorization", "token $token")
}

fun httpFormUrlEncoded(vararg params: Pair<String, String>) =
    params.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, Charsets.UTF_8)}" }

fun String.toBase64(): String = Base64.getEncoder().encodeToString(toByteArray())
