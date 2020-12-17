package org.hildan.github.secrets.wizard.providers.sonatype

import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hildan.github.secrets.wizard.http.basicAuthHeader
import org.hildan.github.secrets.wizard.http.ktorClient
import org.hildan.github.secrets.wizard.http.toBase64
import org.hildan.github.secrets.wizard.providers.Secret
import org.hildan.github.secrets.wizard.providers.SecretOptionGroup
import org.hildan.github.secrets.wizard.providers.SecretProvider

open class SonatypeSecretsDefinition : SecretOptionGroup(
    providerName = "OSS Sonatype",
    switchName = "--sonatype",
) {
    override val secretNames: List<String>
        get() = listOf(userTokenSecretName, keySecretName)

    protected val userTokenSecretName by option(
        "--ossrh-user-secret-name",
        help = "The name of the secret variable holding the OSS Sonatype user token",
    ).default("OSSRH_USER_TOKEN")

    protected val keySecretName by option(
        "--ossrh-key-secret-name",
        help = "The name of the secret variable holding the OSS Sonatype API key",
    ).default("OSSRH_KEY")
}

class SonatypeProvider(val defaultUserLazy: () -> String?) : SonatypeSecretsDefinition(), SecretProvider {

    private val user by option(
        "--ossrh-login",
        help = "Your OSS Sonatype login (defaults to the GitHub username)",
    ).defaultLazy {
        val defaultUser = defaultUserLazy()
        TermUi.prompt("Your OSS Sonatype login", default = defaultUser) ?: defaultUser ?: error("OSS Sonatype user required")
    }

    private val password by option(
        "--ossrh-password",
        help = "Your OSS Sonatype password",
    ).prompt(
        text = "Your OSS Sonatype password",
        hideInput = true,
    )

    override suspend fun fetchSecrets(): List<Secret> {
        val sonatypeKeys = fetchKeys(user, password)
        val userSecret = Secret(userTokenSecretName, sonatypeKeys.userToken)
        val keySecret = Secret(keySecretName, sonatypeKeys.apiKey)
        return listOf(userSecret, keySecret)
    }

    private suspend fun fetchKeys(login: String, password: String): SonatypeKeys {
        val client = ktorClient()

        client.get<String>("https://oss.sonatype.org/service/local/authentication/login") {
            accept(ContentType.Application.Json)
            basicAuthHeader(login, password)
        }

        val loginResponse = client.post<SonatypeTokenResponse>(
            "https://oss.sonatype.org/service/siesta/wonderland/authenticate"
        ) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            body = SonatypeTokenRequest(login.toBase64(), password.toBase64())
        }

        return client.get("https://oss.sonatype.org/service/siesta/usertoken/current") {
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
