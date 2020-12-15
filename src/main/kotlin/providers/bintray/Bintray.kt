package org.hildan.github.secrets.wizard.providers.bintray

import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import io.ktor.client.request.*
import io.ktor.http.*
import org.hildan.github.secrets.wizard.http.httpFormUrlEncoded
import org.hildan.github.secrets.wizard.http.ktorClient
import org.hildan.github.secrets.wizard.providers.Secret
import org.hildan.github.secrets.wizard.providers.SecretProvider
import java.net.URLEncoder

class BintrayOptions(val defaultUserLazy: () -> String?) : OptionGroup(name = "Options for Bintray secrets") {

    val user by option(
        "--bintray-login",
        help = "Your Bintray login (defaults to the GitHub username)"
    ).defaultLazy {
        val defaultUser = defaultUserLazy()
        TermUi.prompt("Your Bintray login", default = defaultUser) ?: defaultUser ?: error("Bintray user required")
    }

    val password by option("--bintray-password", help = "Your Bintray password").prompt(
        text = "Your Bintray password",
        hideInput = true,
    )

    val userSecretName by option(
        "--bintray-user-secret-name",
        help = "The name of the secret variable holding the Bintray username",
    ).default("BINTRAY_USER")

    val keySecretName by option(
        "--bintray-key-secret-name",
        help = "The name of the secret variable holding the Bintray API key",
    ).default("BINTRAY_KEY")
}

object Bintray : SecretProvider<BintrayOptions> {

    override val name: String = "Bintray"

    override fun options(defaultUserLazy: () -> String?) = BintrayOptions(defaultUserLazy)

    override suspend fun fetchSecrets(options: BintrayOptions): List<Secret> = with(options) {
        val apiKey = fetchApiKey(user, password)
        val userSecret = Secret(userSecretName, user)
        val apiKeySecret = Secret(keySecretName, apiKey)
        listOf(userSecret, apiKeySecret)
    }

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
