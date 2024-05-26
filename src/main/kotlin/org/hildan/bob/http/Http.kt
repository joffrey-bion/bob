package org.hildan.bob.http

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

val http = HttpClient(Apache) {
    followRedirects = true
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    useDefaultTransformers = false
    install(HttpCookies) {
        storage = AcceptAllCookiesStorage()
    }
}
