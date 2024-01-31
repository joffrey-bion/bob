package org.hildan.bob.commands

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.mordant.terminal.*
import kotlinx.coroutines.*
import org.hildan.bob.secrets.*
import org.hildan.bob.services.github.*
import org.hildan.bob.utils.*
import java.util.*
import kotlin.system.*

const val GITHUB_USER = "GITHUB_USER"
private const val GITHUB_TOKEN = "GITHUB_TOKEN"

class SetGitHubSecretsCommand : CliktCommand(
    name = "set-github-secrets",
    help = "Sets repository secrets on GitHub by fetching keys from various providers (Bintray, OSS Sonatype, ...)",
) {
    private val githubRepo by option("-r", "--github-repo")
        .help("The GitHub repository to set secrets for")
        .required()

    private val githubUser by option("-u", "--github-user", envvar = GITHUB_USER)
        .help("Your GitHub username or organization. Defaults to the $GITHUB_USER environment variable, or prompts for a value.")
        .prompt("Your GitHub username or organization")

    private val githubToken by option("-t", "--github-token", envvar = GITHUB_TOKEN)
        .help("The token to use to authenticate with GitHub (GitHub doesn't allow password authentication anymore). " +
            "Defaults to the $GITHUB_TOKEN environment variable, or triggers the creation of a new personal token.")
        .defaultLazy {
            runBlocking { setupAndGetToken() }
        }

    private val dryRun by option(
        help = "Enables dry-run mode. In this mode, the secrets won't actually be set on the repository, but the " +
            "secrets will be retrieved from the source and printed.",
    ).flag()

    private val personalToken by option(help = "Enables setting the Personal Access Token as a repo secret")
        .groupSwitch("--set-pat" to GitHubPersonalTokenOptions())

    private val rawSecrets: Map<String, String> by option("-s", "--secret")
        .help("A raw secret to set, in the form KEY=VALUE (this option can be repeated multiple times)")
        .associate()

    private val envSecrets: List<String> by option("-e", "--env")
        .help("A secret read from an environment variable of the same name (can be repeated multiple times)")
        .multiple()

    private val stdinSecret: String? by option( "--stdin")
        .help("The name of a secret, the value of which will be read from stdin")

    private val heroku by secretsDefinitionGroupSwitch(HerokuSecretsDefinition())

    private val sonatype by secretsDefinitionGroupSwitch(SonatypeSecretsDefinition())

    private val definitions: List<SecretGroupDefinition>
        get() = listOfNotNull(heroku, sonatype)

    override fun run() = runBlocking {
        val gitHub = GitHub.login(githubToken)
        val repo = GitHubRepo(githubUser, githubRepo)

        val secrets = buildList {
            personalToken?.let { add(Secret(it.secretName, githubToken)) }

            addAll(definitions.flatMap { it.secretNames.map { s -> Secret(s, System.getenv(s)) } })

            rawSecrets.forEach { (key, value) -> add(Secret(key, value)) }
            envSecrets.forEach { key -> add(Secret(key, System.getenv(key) ?: error("Env var $key not found"))) }

            stdinSecret?.let { key -> add(Secret(key, value = readStdIn())) }
        }

        println("Setting secrets in GitHub repository $githubRepo...")
        if (dryRun) {
            println("DRY-RUN: would have set the following secrets:")
            secrets.forEach { println("${it.name}=${it.value}") }
        } else {
            secrets.forEach {
                gitHub.setSecret(it, repo)
                echo("Secret ${it.name} set")
            }
        }
    }

    private suspend fun setupAndGetToken(): String {
        println("$GITHUB_TOKEN is not set, please create a new token at ${GitHub.newTokenUrl}")
        browseIfSupported(GitHub.newTokenUrl)
        val token = terminal.prompt("Enter Personal Access Token", hideInput = true)
        if (token.isNullOrBlank()) {
            System.err.println("No token provided. Aborting.")
            exitProcess(1)
        }
        askAndStore(token)
        return token
    }

    private suspend fun askAndStore(token: String) {
        if (!OS.isWindows) {
            return // this is only supported on windows
        }
        val shouldStore = YesNoPrompt(
            prompt = "Would you like to store the token in $GITHUB_TOKEN environment variable?",
            terminal = terminal,
            default = true,
        ).ask() ?: true // for non-interactive

        if (shouldStore) {
            setWindowsEnv(GITHUB_TOKEN, token)
        }
    }
}

private fun readStdIn(): String = buildString {
    val sc = Scanner(System.`in`)
    while (sc.hasNextLine()) {
        append(sc.nextLine())
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
