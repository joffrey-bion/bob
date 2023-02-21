package org.hildan.bob.services.sonatype

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.*
import org.hildan.bob.http.*

object Sonatype {

    suspend fun fetchKeys(login: String, password: String): SonatypeKeys {
        http.get("https://oss.sonatype.org/service/local/authentication/login") {
            accept(ContentType.Application.Json)
            basicAuth(login, password)
        }

        val loginResponse = http.post("https://oss.sonatype.org/service/siesta/wonderland/authenticate") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(SonatypeTokenRequest(login.toBase64(), password.toBase64()))
        }.body<SonatypeTokenResponse>()

        return http.get("https://oss.sonatype.org/service/siesta/usertoken/current") {
            contentType(ContentType.Application.Json)
            header("x-nexus-ui", "true")
            header("x-nx-authticket", loginResponse.token)
        }.body()
    }
}

@Serializable
data class SonatypeTokenRequest(
    @SerialName("u")
    val user: String,
    @SerialName("p")
    val password: String,
)

@Serializable
data class SonatypeTokenResponse(
    @SerialName("t")
    val token: String,
)

@Serializable
data class SonatypeKeys(
    @SerialName("nameCode")
    val userToken: String,
    @SerialName("passCode")
    val apiKey: String,
)
