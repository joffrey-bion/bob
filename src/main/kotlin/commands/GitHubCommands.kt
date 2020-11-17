package org.hildan.github.secrets.wizard.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupSwitch
import com.github.ajalt.clikt.parameters.options.*
import kotlinx.coroutines.runBlocking
import org.hildan.github.secrets.wizard.GitHub
import org.hildan.github.secrets.wizard.GitHubRepo
import org.hildan.github.secrets.wizard.browseIfSupported
import org.hildan.github.secrets.wizard.providers.Bintray
import org.hildan.github.secrets.wizard.providers.OssSonatype
import org.hildan.github.secrets.wizard.setWindowsEnv
import kotlin.system.exitProcess

private const val GITHUB_USER = "GITHUB_USER"
private const val GITHUB_TOKEN = "GITHUB_TOKEN"

class GitHubSecretCommand : CliktCommand(
    name = "set-github-secrets",
    help = "Sets repository secrets on GitHub by fetching keys from various providers (Bintray, OSS Sonatype, ...)",
) {
    private val githubRepo by option(
        "-r",
        "--github-repo",
        help = "The GitHub repository to set secrets for",
    ).required()

    private val githubUser by option(
        "-u",
        "--github-user",
        envvar = GITHUB_USER,
        help = "Your GitHub username or organization. " +
            "Defaults to the $GITHUB_USER environment variable, or prompts for a value.",
    ).prompt("Your GitHub username or organization")

    private val githubToken by option(
        "-t",
        "--github-token",
        envvar = GITHUB_TOKEN,
        help = "The token to use to authenticate with GitHub (GitHub doesn't allow password authentication anymore). " +
            "Defaults to the $GITHUB_TOKEN environment variable, or triggers the creation of a new personal token.",
    )

    private val bintray by option(help = "Enables Bintray secrets setup").groupSwitch("--bintray" to BintraySecretOptions())

    private val sonatype by option(help = "Enables OSS Sonatype (Maven Central) secrets setup").groupSwitch("--sonatype" to SonatypeSecretOptions())

    private val rawSecrets: Map<String, String> by option(
        "-s",
        "--secret",
        help = "A raw secret to set, in the form KEY=VALUE (this option can be repeated multiple times)"
    ).associate()

    override fun run() = runBlocking {
        println("Setting secrets in GitHub repository $githubRepo")

        val gitHub = GitHub.login(githubToken ?: setupAndGetToken())
        val repo = GitHubRepo(githubUser, githubRepo)

        bintray?.let {
            print("Fetching API key from Bintray...")
            val bintrayUser = it.user.ifEmpty { githubUser }
            val bintrayKey = Bintray.fetchApiKey(bintrayUser, it.password)
            println("Done.")
            gitHub.setSecret(it.userSecretName, bintrayUser, repo)
            gitHub.setSecret(it.keySecretName, bintrayKey, repo)
        }

        sonatype?.let {
            print("Fetching user token and API key from OSS Sonatype...")
            val sonatypeUser = it.user.ifEmpty { githubUser }
            val sonatypeKeys = OssSonatype.fetchKeys(sonatypeUser, it.password)
            println("Done.")
            gitHub.setSecret(it.userTokenSecretName, sonatypeKeys.userToken, repo)
            gitHub.setSecret(it.keySecretName, sonatypeKeys.apiKey, repo)
        }

        rawSecrets.forEach { (key, value) ->
            gitHub.setSecret(key, value, repo)
        }
    }

    private suspend fun setupAndGetToken(): String {
        println("$GITHUB_TOKEN is not set, please create a new token at ${GitHub.newTokenUrl}")
        browseIfSupported(GitHub.newTokenUrl)
        val token = TermUi.prompt("Enter Personal Access Token", hideInput = true)
        if (token.isNullOrBlank()) {
            System.err.println("No token provided. Aborting.")
            exitProcess(1)
        }
        askAndStore(token)
        return token
    }

    private suspend fun askAndStore(token: String) {
        if ("Windows" !in System.getProperty("os.name")) {
            return // this is only supported on windows
        }
        val shouldStore = TermUi.confirm(
            text = "Would you like to store the token in $GITHUB_TOKEN environment variable? (Y/n)",
            default = true,
        ) ?: true // for non-interactive

        if (shouldStore) {
            setWindowsEnv(GITHUB_TOKEN, token)
        }
    }
}

private class BintraySecretOptions : OptionGroup(
    name = "Options for Bintray secrets",
    help = "Options to setup the Bintray API key as GitHub secret (enable with --bintray)",
) {
    val user by option("--bintray-login", help = "Your Bintray login (defaults to the GitHub username)").prompt(
        text = "Your Bintray login",
        default = "",
    )

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

private class SonatypeSecretOptions : OptionGroup(
    name = "Options for OSS Sonatype secrets",
    help = "Options to setup the OSS Sonatype user token and key as GitHub secrets (enable with --sonatype)",
) {
    val user by option("--ossrh-login", help = "Your OSS Sonatype login (defaults to the GitHub username)").prompt(
        text = "Your OSS Sonatype login",
        default = "",
    )

    val password by option("--ossrh-password", help = "Your OSS Sonatype password").prompt(
        text = "Your OSS Sonatype password",
        hideInput = true,
    )

    val userTokenSecretName by option(
        "--ossrh-user-secret-name",
        help = "The name of the secret variable holding the OSS Sonatype user token",
    ).default("OSSRH_USER_TOKEN")

    val keySecretName by option(
        "--ossrh-key-secret-name",
        help = "The name of the secret variable holding the OSS Sonatype API key",
    ).default("OSSRH_KEY")
}
