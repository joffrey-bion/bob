package org.hildan.bob.commands

import com.charleskorn.kaml.*
import com.github.ajalt.clikt.command.*
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import kotlinx.serialization.*
import org.hildan.bob.services.github.*
import java.nio.file.Path
import kotlin.io.path.*

class SetGitHubSecretsCommand : SuspendingCliktCommand(name = "set-github-secrets") {

    private val githubToken by option("-t", "--github-token", envvar = "GITHUB_TOKEN")
        .help("The token to use to authenticate with GitHub (GitHub doesn't allow password authentication anymore). " +
            "Defaults to the GITHUB_TOKEN environment variable.")
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

        val secretsDefinitions = definitionsFile.readDefinitions()

        secretsDefinitions.secretsByRepo().forEach { (repoName, secrets) ->
            gitHub.setSecrets(GitHubRepo(secretsDefinitions.user, repoName), secrets)
        }
    }

    private suspend fun GitHub.setSecrets(repo: GitHubRepo, secrets: List<Secret>) {
        println("Setting secrets in GitHub repository ${repo.slug}...")
        if (dryRun) {
            println("DRY-RUN: would have set the following secrets:")
            secrets.forEach { println("  ${it.name}=${it.value} (${it.type})") }
        } else {
            secrets.forEach {
                setSecret(it, repo)
                echo("  ${it.name} (${it.type})")
            }
        }
    }
}

private fun Path.readDefinitions(): GitHubSecretsDefinition = inputStream().use { Yaml.default.decodeFromStream(it) }

private fun GitHubSecretsDefinition.secretsByRepo() = repositories.mapValues { (repo, profile) ->
    val bundles = profiles[profile]
        ?: throw PrintMessage("Invalid YAML: unknown profile '$profile' used in repository '$repo'", printError = true)
    bundles.flatMap { b ->
        secretBundles[b]?.map { resolveSecret(it) }
            ?: throw PrintMessage("Invalid YAML: unknown bundle '$b' used in repository '$repo'", printError = true)
    }
}

private fun resolveSecret(def: SecretDefinition): Secret {
    val value = System.getenv(def.sourceEnv)
        ?: throw PrintMessage("Secret environment variable ${def.sourceEnv} is not set", printError = true)
    return Secret(def.secret, value, type = def.type)
}

@Serializable
private class GitHubSecretsDefinition(
    val user: String,
    val secretBundles: Map<String, List<SecretDefinition>>,
    val profiles: Map<String, List<String>>,
    val repositories: Map<String, String>,
)

@Serializable
private class SecretDefinition(
    val secret: String,
    val sourceEnv: String = secret,
    val type: SecretType = SecretType.actions,
)
