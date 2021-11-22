package org.hildan.bob.services.sonatype

import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hildan.bob.http.basicAuthHeader
import org.hildan.bob.http.http
import org.hildan.bob.http.toBase64

object Sonatype {

    suspend fun fetchKeys(login: String, password: String): SonatypeKeys {
        http.get<String>("https://oss.sonatype.org/service/local/authentication/login") {
            accept(ContentType.Application.Json)
            basicAuthHeader(login, password)
        }

        val loginResponse =
            http.post<SonatypeTokenResponse>("https://oss.sonatype.org/service/siesta/wonderland/authenticate") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                body = SonatypeTokenRequest(login.toBase64(), password.toBase64())
            }

        return http.get("https://oss.sonatype.org/service/siesta/usertoken/current") {
            contentType(ContentType.Application.Json)
            header("x-nexus-ui", "true")
            header("x-nx-authticket", loginResponse.token)
        }
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
