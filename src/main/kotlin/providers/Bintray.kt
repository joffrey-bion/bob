package org.hildan.github.secrets.wizard.providers

import io.ktor.client.request.*
import io.ktor.http.*
import org.hildan.github.secrets.wizard.http.httpFormUrlEncoded
import org.hildan.github.secrets.wizard.http.ktorClient
import java.net.URLEncoder

object Bintray {

    suspend fun fetchApiKey(login: String, password: String): String {
        val client = ktorClient()

        // Login to store authentication cookie
        client.post<Unit>("https://bintray.com/user/JSONLogin") {
            contentType(ContentType.Application.FormUrlEncoded)
            body = httpFormUrlEncoded("login" to login, "password" to password)
        }

        val urlPassword = URLEncoder.encode(password, Charsets.UTF_8)
        val response = client.get<String>("https://bintray.com/user/edit/tab/apikey?password=$urlPassword")
        val match =
            Regex("""<div id="apiKeyText" class="apiKeyText" data-key="([^"]+)">""").find(response)
                ?: error("Couldn't find Bintray API key in HTML response")
        return match.groupValues[1]
    }
}
