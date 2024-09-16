package org.hildan.bob.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.hildan.bob.services.gradle.Gradle
import org.hildan.bob.utils.OS
import org.hildan.bob.utils.exec

class UpgradeGradleWrapperCommand : CliktCommand(name = "upgrade-gradle-wrapper") {

    private val version by option(
        "-v",
        "--gradle-version",
        help = "The gradle version to use"
    )

    private val commit by option(help = "Commit the modified files").flag("--no-commit", default = true)

    override fun help(context: Context): String = "Upgrades the gradle wrapper to the latest version"

    override fun run(): Unit = runBlocking {
        val versionDetails = fetchVersionDetails(version)

        val checksum = logAndDo("Fetching checksum...", sameLine = true) {
            versionDetails.fetchChecksum()
        }
        val effectiveVersion = versionDetails.version
        logAndDo("Updating wrapper configuration...") {
            runWrapperCommand(effectiveVersion, checksum)
        }
        logAndDo("Updating wrapper binaries...") {
            runWrapperCommand(effectiveVersion, checksum)
        }

        if (commit) {
            exec(
                "git",
                "commit",
                "-m",
                "\"Upgrade Gradle wrapper to $effectiveVersion\"",
                "gradlew",
                "gradlew.bat",
                "gradle/wrapper",
            )
        }
    }

    private suspend fun fetchVersionDetails(version: String?) = if (version != null) {
        logAndDo("Fetching version details for $version...", sameLine = true) {
            Gradle.getVersion(version)
        }
    } else {
        logAndDo("Fetching latest Gradle version...", sameLine = true) {
            Gradle.getLatestVersion()
        }.also {
            echo("Latest version: ${it.version}")
        }
    }

    private inline fun <T> logAndDo(text: String, sameLine: Boolean = false, block: () -> T): T {
        echo(text, trailingNewline = !sameLine)
        return block().also { println("Done.") }
    }

    private suspend fun runWrapperCommand(version: String, checksum: String) {
        exec(
            if (OS.isWindows) "gradlew.bat" else "gradlew",
            "wrapper",
            "--gradle-version",
            version,
            "--gradle-distribution-sha256-sum",
            checksum,
        )
    }
}
