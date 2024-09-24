package org.hildan.bob

import com.github.ajalt.clikt.command.*
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.output.*
import org.hildan.bob.commands.*

suspend fun main(args: Array<String>) = Cli().subcommands(
    SetGitHubSecretsCommand(),
    UpgradeGradleWrapperCommand(),
    ListKotlinPlatformsCommand(),
).main(args)

class Cli : SuspendingCliktCommand(name = "bob") {

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true) }
        }
    }

    override fun help(context: Context): String = "A helper to manage and maintain projects"

    override fun aliases(): Map<String, List<String>> = mapOf(
        "ugw" to listOf("upgrade-gradle-wrapper")
    )

    override suspend fun run() = Unit
}
