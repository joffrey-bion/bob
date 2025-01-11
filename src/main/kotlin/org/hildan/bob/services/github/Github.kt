package org.hildan.bob.services.github

import com.goterl.lazysodium.*
import com.goterl.lazysodium.utils.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.*
import org.hildan.bob.http.*
import java.util.*

data class GitHubRepo(
    val userOrOrg: String,
    val name: String,
) {
    val slug = "$userOrOrg/$name"
}

data class Secret(
    val name: String,
    val value: String,
    val type: SecretType,
)

enum class SecretType(val urlValue: String) {
    actions("actions"),
    dependabot("dependabot")
}

data class GitHub(
    val token: String,
) {
    private val ghClient: HttpClient = http.config {
        install(DefaultRequest) {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.github.com"
            }
            bearerAuth(token)
            accept(ContentType.Application.Json)
        }
    }

    private val lazySodium = LazySodiumJava(SodiumJava())

    suspend fun setSecret(secret: Secret, repo: GitHubRepo) {
        val publicKey = fetchPublicKey(repo, secretType = secret.type)
        val encryptedHexa = lazySodium.cryptoBoxSealEasy(secret.value, publicKey.asLibsodiumKey())
        ghClient.put {
            url { encodedPath = "/repos/${repo.slug}/${secret.type.urlValue}/secrets/${secret.name}" }
            contentType(ContentType.Application.Json)
            setBody(GitHubCreateSecretRequest(publicKey.id, encryptedHexa.hexadecimalToBase64()))
        }
    }

    private suspend fun fetchPublicKey(repo: GitHubRepo, secretType: SecretType): GitHubPublicKeyResponse = ghClient.get {
        url { encodedPath = "/repos/${repo.slug}/${secretType.urlValue}/secrets/public-key" }
    }.body()

    companion object {

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

private fun String.hexadecimalToBase64() = Base64.getEncoder().encodeToString(byteArrayFromHexString())

@OptIn(ExperimentalUnsignedTypes::class)
private fun String.byteArrayFromHexString() = chunked(2)
    .map { it.toUByte(16) }
    .toUByteArray()
    .toByteArray()
