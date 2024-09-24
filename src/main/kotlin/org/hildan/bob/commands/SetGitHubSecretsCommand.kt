package org.hildan.bob.commands

import com.charleskorn.kaml.*
import com.github.ajalt.clikt.command.*
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import kotlinx.serialization.*
import org.hildan.bob.services.github.*
import kotlin.io.path.*

class SetGitHubSecretsCommand : SuspendingCliktCommand(name = "set-github-secrets") {

    private val githubToken by option("-t", "--github-token", envvar = "GITHUB_TOKEN")
        .help("The token to use to authenticate with GitHub (GitHub doesn't allow password authentication anymore). " +
            "Defaults to the GITHUB_TOKEN environment variable, or triggers the creation of a new personal token.")
        .required()

    private val definitionsFile by option("-f", "--file")
        .help("The definitions file describing the secrets required by each repository")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)
        .required()

    private val dryRun by option("--dry-run")
        .help("Enables dry-run mode. In this mode, the secrets won't actually be set on the repository, " +
                "but the secrets will be retrieved from the source and printed.")
        .flag()

    override fun help(context: Context) = "Sets secrets on GitHub repositories based on a file definition"

    override suspend fun run() {
        val gitHub = GitHub.login(githubToken)

        val secretsDefinitions = Yaml.default.decodeFromStream<GitHubSecretsDefinition>(definitionsFile.inputStream())
        val secretsByRepo = secretsDefinitions.secretsByRepo()

        secretsByRepo.forEach { (repoName, secrets) ->
            gitHub.setSecrets(GitHubRepo(secretsDefinitions.user, repoName), secrets)
        }
    }

    private suspend fun GitHub.setSecrets(repo: GitHubRepo, secrets: List<Secret>) {
        println("Setting secrets in GitHub repository ${repo.slug}...")
        if (dryRun) {
            println("DRY-RUN: would have set the following secrets:")
            secrets.forEach { println("${it.name}=${it.value}") }
        } else {
            secrets.forEach {
                setSecret(it, repo)
                echo("Secret ${it.name} set")
            }
        }
    }
}

private fun GitHubSecretsDefinition.secretsByRepo() = repositories.mapValues { (repo, def) ->
    def.bundles.flatMap { b ->
        secretBundles[b]?.map { resolveSecret(it) }
            ?: throw PrintMessage("Invalid YAML: unknown bundle '$b' used in repository '$repo'", printError = true)
    }
}

private fun resolveSecret(envVarName: String): Secret {
    val value = System.getenv(envVarName)
        ?: throw PrintMessage("Secret environment variable $envVarName is not set", printError = true)
    return Secret(envVarName, value)
}

@Serializable
private class GitHubSecretsDefinition(
    val user: String,
    val secretBundles: Map<String, List<String>>,
    val repositories: Map<String, RepoDefinition>,
)

@Serializable
private class RepoDefinition(
    val bundles: List<String>,
)
