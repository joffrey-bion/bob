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

const val GITHUB_USER = "GITHUB_USER"
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
    ).defaultLazy {
        runBlocking { setupAndGetToken() }
    }

    private val dryRun by option(
        help = "Enables dry-run mode. In this mode, the secrets won't actually be set on the repository, but the keys" +
            " will be retrieved from providers and the GitHub login will happen as well."
    ).flag()

    private val personalToken by option(help = "Enables setting the Personal Access Token as a repo secret")
        .groupSwitch("--set-pat" to GitHubPersonalTokenOptions())

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
        println("Setting secrets in GitHub repository $githubRepo")

        val gitHub = GitHub.login(githubToken, dryRun = dryRun)
        val repo = GitHubRepo(githubUser, githubRepo)

        personalToken?.let {
            gitHub.setSecret(it.secretName, githubToken, repo)
        }

        bintray?.let {
            print("Fetching API key from Bintray...")
            val bintrayKey = Bintray.fetchApiKey(it.user, it.password)
            println("Done.")
            gitHub.setSecret(it.userSecretName, it.user, repo)
            gitHub.setSecret(it.keySecretName, bintrayKey, repo)
        }

        sonatype?.let {
            print("Fetching user token and API key from OSS Sonatype...")
            val sonatypeKeys = OssSonatype.fetchKeys(it.user, it.password)
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

private class GitHubPersonalTokenOptions : OptionGroup(
    name = "Options for GitHub personal token"
) {
    val secretName by option(
        "--github-token-secret-name",
        help = "The name of the secret variable holding the GitHub Personal Access Token (PAT)",
    ).default("PAT")
}
