package org.hildan.bob.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.hildan.bob.services.gradle.Gradle
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
        val versionDetails = version?.let { Gradle.getVersion(it) } ?: Gradle.getLatestVersion()
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
