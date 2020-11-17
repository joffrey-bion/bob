package org.hildan.github.secrets.wizard

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import com.goterl.lazycode.lazysodium.utils.Key
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.hildan.github.secrets.wizard.http.ktorClient
import org.hildan.github.secrets.wizard.http.oAuthHeader
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
            oAuthHeader(token)
            accept(ContentType.Application.Json)
        }
    }
    private val lazySodium = LazySodiumJava(SodiumJava())

    suspend fun setSecret(secretName: String, secretValue: String, repo: GitHubRepo) {
        println("Setting secret $secretName...")
        val publicKey = fetchPublicKey(repo)
        val encryptedHexa = lazySodium.cryptoBoxSealEasy(secretValue, publicKey.asLibsodiumKey())
        ghClient.put<Unit> {
            url { encodedPath = "/repos/${repo.slug}/actions/secrets/$secretName" }
            contentType(ContentType.Application.Json)
            body = GitHubCreateSecretRequest(publicKey.id, encryptedHexa.hexadecimalToBase64())
        }
    }

    private suspend fun fetchPublicKey(repo: GitHubRepo): GitHubPublicKeyResponse = ghClient.get {
        url { encodedPath = "/repos/${repo.slug}/actions/secrets/public-key" }
    }

    companion object {
        const val newTokenUrl = "https://github.com/settings/tokens/new?description=GitHub%20Secrets%20Wizard&scopes=repo"

        fun login(token: String): GitHub = GitHub(token)
    }
}

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

private fun String.hexadecimalToBase64() = byteArrayFromHexString().toBase64()

private fun String.byteArrayFromHexString() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

private fun ByteArray.toBase64() = Base64.getEncoder().encodeToString(this)