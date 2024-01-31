package org.hildan.bob.services.heroku

import com.github.ajalt.clikt.core.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.*
import org.hildan.bob.http.*

object Heroku {

    suspend fun fetchApiKey(login: String, password: String): String {
        http.setupCookiesWithCredentialsLogin(login, password)
        val token = http.oAuthLogin()

        val response = http.get("https://api.heroku.com/oauth/authorizations/~?name=~") {
            accept(ContentType.parse("application/vnd.heroku+json; version=3"))
            bearerAuth(token)
        }.body<AuthResponse>()
        return response.accessToken?.token ?: error("API key not found, was it ever generated?")
    }

    private suspend fun HttpClient.setupCookiesWithCredentialsLogin(login: String, password: String) {
        // Login to store authentication cookie
        val html = get("https://id.heroku.com/login").body<String>()
        val csrfToken = html.extractCsrf()

        try {
            // Login to store authentication cookie
            post("https://id.heroku.com/login") {
                header("Referer", "https://id.heroku.com/login")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(httpFormUrlEncoded(
                    "_csrf" to csrfToken,
                    "email" to login,
                    "password" to password,
                    "commit" to "Log In",
                ))
            }
        } catch (e: RedirectResponseException) {
            // ignore the redirection, we don't want to follow it
        }
    }

    private suspend fun HttpClient.oAuthLogin(): String {
        val response: HttpResponse = get("https://auth.heroku.com/login?state=https%3A%2F%2Fdashboard.heroku.com%2Fauth%2Fheroku%2Fcallback") {
            accept(ContentType.Text.Html)
        }
        val oAuthCode = response.request.url.parameters["code"] ?:
            throw PrintMessage("No OAuth code received from Heroku", printError = true)
        val tokenResponse = post("https://auth.heroku.com/login/token") {
            contentType(ContentType.Application.FormUrlEncoded)

            setBody(httpFormUrlEncoded(
                "grant_type" to "password",
                "username" to "null",
                "password" to oAuthCode,
            ))
        }.body<OAuthTokenResponse>()
        return tokenResponse.accessToken ?: throw PrintMessage("No OAuth token received from Heroku", printError = true)
    }
}

@Serializable
data class OAuthTokenResponse(
    @SerialName("access_token")
    val accessToken: String?
)

@Serializable
data class AuthResponse(
    @SerialName("access_token")
    val accessToken: AccessTokenData?
)

@Serializable
data class AccessTokenData(
    val id: String,
    val token: String?
)

private val htmlCsrfRegex = Regex("""<\s*input\s+name="_csrf"\s+type="hidden"\s+value="([^"]+)"""")

private fun String.extractCsrf(): String {
    val result = htmlCsrfRegex.find(this) ?: error("Couldn't find CSRF token in login form")
    return result.groupValues[1]
}
