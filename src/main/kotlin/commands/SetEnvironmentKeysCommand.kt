package org.hildan.github.secrets.wizard.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.groupSwitch
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import kotlinx.coroutines.runBlocking
import org.hildan.github.secrets.wizard.providers.Bintray
import org.hildan.github.secrets.wizard.providers.OssSonatype
import org.hildan.github.secrets.wizard.setWindowsEnv

class SetEnvironmentKeysCommand : CliktCommand(
    name = "set-env-secrets",
    help = "Sets secrets from various providers (Bintray, OSS Sonatype, ...) as environment variables (windows only)",
) {
    private val githubUser by option(
        "-u",
        "--default-user",
        envvar = GITHUB_USER,
        help = "The default user to suggest for all providers. " +
            "Defaults to the $GITHUB_USER environment variable, or prompts for a value.",
    ).prompt("Your GitHub username or organization")

    private val bintray by option(help = "Enables Bintray secrets setup")
        .groupSwitch("--bintray" to BintraySecretOptions { githubUser })

    private val sonatype by option(help = "Enables OSS Sonatype (Maven Central) secrets setup")
        .groupSwitch("--sonatype" to SonatypeSecretOptions { githubUser })

    private val rawSecrets: Map<String, String> by option(
        "-s",
        "--secret",
        help = "A raw secret to set, in the form KEY=VALUE (this option can be repeated multiple times)"
    ).associate()

    override fun run() = runBlocking {
        println("Setting secrets in environment variables...")

        bintray?.let {
            print("Fetching API key from Bintray...")
            val bintrayUser = it.user.ifEmpty { githubUser }
            val bintrayKey = Bintray.fetchApiKey(bintrayUser, it.password)
            println("Done.")
            setWindowsEnv(it.userSecretName, bintrayUser)
            setWindowsEnv(it.keySecretName, bintrayKey)
        }

        sonatype?.let {
            print("Fetching user token and API key from OSS Sonatype...")
            val sonatypeUser = it.user.ifEmpty { githubUser }
            val sonatypeKeys = OssSonatype.fetchKeys(sonatypeUser, it.password)
            println("Done.")
            setWindowsEnv(it.userTokenSecretName, sonatypeKeys.userToken)
            setWindowsEnv(it.keySecretName, sonatypeKeys.apiKey)
        }

        rawSecrets.forEach { (key, value) ->
            setWindowsEnv(key, value)
        }
    }
}
