package org.hildan.bob.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.hildan.bob.http.http
import org.hildan.bob.utils.OS
import org.hildan.bob.utils.exec

class UpgradeGradleWrapperCommand : CliktCommand(
    name = "upgrade-gradle-wrapper",
    help = "Upgrades the gradle wrapper to the latest version",
) {
    private val version by option(
        "-v",
        "--gradle-version",
        help = "The gradle version to use"
    )

    // private val commit by option(help = "Commit the modified files").flag("--no-commit", default = true)

    override fun run(): Unit = runBlocking {
        val versionDetails = version?.let { GradleApi.getVersion(it) } ?: GradleApi.getLatestVersion()
        val checksum = versionDetails.fetchChecksum()
        exec(
            if (OS.isWindows) "gradlew.bat" else "./gradlew",
            "wrapper",
            "--gradle-version",
            versionDetails.version,
            "--gradle-distribution-sha256-sum",
            checksum,
        )
    }
}

object GradleApi {

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
