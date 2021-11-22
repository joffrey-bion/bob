package org.hildan.bob.services.gradle

import io.ktor.client.request.*
import kotlinx.serialization.Serializable
import org.hildan.bob.http.http

object Gradle {

    suspend fun getLatestVersion(): GradleVersionDetails =
        http.get("https://services.gradle.org/versions/current")

    suspend fun getVersion(version: String): GradleVersionDetails {
        val allVersions = http.get<List<GradleVersionDetails>>("https://services.gradle.org/versions/all")
        return allVersions.find { it.version == version } ?: error("Gradle version $version not found")
    }
}

@Serializable
data class GradleVersionDetails(
    val version: String,
    val buildTime: String,
    val current: Boolean,
    val snapshot: Boolean,
    val nightly: Boolean,
    val releaseNightly: Boolean,
    val activeRc: Boolean,
    val rcFor: String,
    val milestoneFor: String,
    val broken: Boolean,
    val downloadUrl: String,
    val checksumUrl: String,
    val wrapperChecksumUrl: String
) {
    suspend fun fetchChecksum() = http.get<String>(checksumUrl)
}
