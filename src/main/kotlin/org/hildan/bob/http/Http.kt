package org.hildan.bob.http

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import java.net.*
import java.util.*

val http = ktorClient()

fun ktorClient(
    logging: Boolean = false,
    configure: HttpClientConfig<ApacheEngineConfig>.() -> Unit = {},
) = HttpClient(Apache) {
    followRedirects = true
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    useDefaultTransformers = false
    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
    }
    if (logging) {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
    }
    configure()
}

fun httpFormUrlEncoded(vararg params: Pair<String, String>) =
    params.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, Charsets.UTF_8)}" }

fun String.toBase64(): String = Base64.getEncoder().encodeToString(toByteArray())
