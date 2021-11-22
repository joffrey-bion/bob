package org.hildan.bob.services.github

import com.github.ajalt.clikt.output.TermUi.echo
import com.goterl.lazysodium.LazySodiumJava
import com.goterl.lazysodium.SodiumJava
import com.goterl.lazysodium.utils.Key
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hildan.bob.http.OAuth
import org.hildan.bob.http.ktorClient
import org.hildan.bob.http.tokenAuthHeader
import org.hildan.bob.secrets.Secret
import java.util.*

data class GitHubRepo(
    val userOrOrg: String,
    val name: String,
) {
    val slug = "$userOrOrg/$name"
}

data class GitHub(
    val token: String,
) {
    private val ghClient: HttpClient = ktorClient {
        install(DefaultRequest) {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.github.com"
            }
            tokenAuthHeader(token)
            accept(ContentType.Application.Json)
        }
    }

    private val lazySodium = LazySodiumJava(SodiumJava())

    suspend fun setSecret(secret: Secret, repo: GitHubRepo) {
        val publicKey = fetchPublicKey(repo)
        val encryptedHexa = lazySodium.cryptoBoxSealEasy(secret.value, publicKey.asLibsodiumKey())
        ghClient.put<Unit> {
            url { encodedPath = "/repos/${repo.slug}/actions/secrets/${secret.name}" }
            contentType(ContentType.Application.Json)
            body = GitHubCreateSecretRequest(publicKey.id, encryptedHexa.hexadecimalToBase64())
        }
        echo("Secret ${secret.name} set")
    }

    private suspend fun fetchPublicKey(repo: GitHubRepo): GitHubPublicKeyResponse = ghClient.get {
        url { encodedPath = "/repos/${repo.slug}/actions/secrets/public-key" }
    }

    companion object {
        const val newTokenUrl = "https://github.com/settings/tokens/new?description=GitHub%20Secrets%20Wizard&scopes=repo"

        fun login(token: String): GitHub = GitHub(token)

        suspend fun loginOAuth(clientId: String, clientSecret: String): GitHub {
            val client = ktorClient {
                followRedirects = false
            }

            val code = OAuth.authorizeInBrowser(
                url = "https://github.com/login/oauth/authorize?client_id=$clientId&scope=repo",
                callbackUriParamName = "redirect_uri",
            )

            val response = client.post<GitHubOAuthResponse>("https://github.com/login/oauth/access_token") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                body = GitHubOAuthRequest(clientId, clientSecret, code)
            }
            return GitHub(response.token)
        }
    }
}

@Serializable
private data class GitHubOAuthRequest(
    @SerialName("client_id")
    val clientId: String,
    @SerialName("client_secret")
    val clientSecret: String,
    @SerialName("code")
    val authorizationCode: String,
)

@Serializable
private data class GitHubOAuthResponse(
    @SerialName("access_token")
    val token: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("scope")
    val scopes: String,
)

@Serializable
private data class GitHubPublicKeyResponse(
    @SerialName("key_id")
    val id: String,
    @SerialName("key")
    val base64Value: String,
) {
    fun asLibsodiumKey(): Key = Key.fromBase64String(base64Value)
}

@Serializable
private data class GitHubCreateSecretRequest(
    @SerialName("key_id")
    val publicKeyId: String,
    @SerialName("encrypted_value")
    val secretValueBase64: String,
)

private fun String.hexadecimalToBase64() = Base64.getEncoder().encodeToString(byteArrayFromHexString())

@OptIn(ExperimentalUnsignedTypes::class)
private fun String.byteArrayFromHexString() = chunked(2)
    .map { it.toUByte(16) }
    .toUByteArray()
    .toByteArray()
